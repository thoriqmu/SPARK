package com.bravy.app.ui.view.practice

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.bravy.app.databinding.ActivityAnxietyTestBinding
import com.bravy.app.ml.AnxietyClassifier
import com.bravy.app.ui.viewmodel.AnxietyTestViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class AnxietyTestActivity : AppCompatActivity(), AnxietyClassifier.ClassifierListener {

    private lateinit var binding: ActivityAnxietyTestBinding
    private val viewModel: AnxietyTestViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var anxietyClassifier: AnxietyClassifier
    private var classificationResults = mutableListOf<String>()

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnxietyTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        anxietyClassifier = AnxietyClassifier(this, listener = this)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Image Analyzer untuk klasifikasi TFLite
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        val bitmap = image.toBitmap()
                        if (bitmap != null) {
                            anxietyClassifier.classify(bitmap)
                        }
                        image.close()
                    }
                }

            // Video Capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture, imageAnalyzer
                )
                startTestTimer()
            } catch (exc: Exception) {
                Log.e(TAG, "Gagal binding use cases", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startTestTimer() {
        captureVideo() // Mulai rekam video
        object : CountDownTimer(30000, 1000) { // 2 menit
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                binding.tvTimer.text = "00:00"
                stopAndUploadVideo()
            }
        }.start()
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return
        val name = "AnxietyTest-${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SparkEdtech-Videos")
            }
        }
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@AnxietyTestActivity, Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    if (!recordEvent.hasError()) {
                        val uri = recordEvent.outputResults.outputUri
                        viewModel.uploadVideo(uri, getFinalClassification())
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                    }
                }
            }
    }

    private fun stopAndUploadVideo() {
        recording?.stop()
        recording = null

        viewModel.uploadResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Upload successful!", Toast.LENGTH_SHORT).show()
                // Kirim hasil kembali ke PracticeActivity
                val intent = android.content.Intent()
                intent.putExtra("ANXIETY_RESULT", getFinalClassification())
                setResult(RESULT_OK, intent)
                finish()
            }.onFailure {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getFinalClassification(): String {
        if (classificationResults.isEmpty()) return "Not Determined"
        // Mengembalikan kelas yang paling sering muncul
        return classificationResults.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Not Determined"
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "AnxietyTestActivity"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    // -- AnxietyClassifier.ClassifierListener Callbacks --
    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResult(label: String, score: Float) {
        // 'label' sekarang adalah string hasil prediksi, contoh: "anxious"
        // 'score' adalah tingkat kepercayaan (0.0 - 1.0)

        classificationResults.add(label)

        // Opsi: Anda bisa menampilkan hasil real-time di UI untuk debugging
        // runOnUiThread { binding.tvInstruction.text = "Current State: $label" }
    }
}