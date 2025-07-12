package com.bravy.app.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType

class AnxietyClassifier(
    private val context: Context,
    private val modelName: String = "kecemasan_model.tflite",
    private val listener: ClassifierListener
) {
    private var interpreter: Interpreter? = null
    // Definisikan label kelas Anda sesuai urutan output model
    private val labels = listOf("Very Relaxing", "Relaxing", "Midly Anxious", "Anxious", "Very Anxious")

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            val model = FileUtil.loadMappedFile(context, modelName)
            val options = Interpreter.Options()
            options.setNumThreads(4) // Optimasi untuk performa
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            listener.onError("Gagal memuat model TFLite: ${e.message}")
        }
    }

    fun classify(image: Bitmap) {
        if (interpreter == null) {
            listener.onError("Interpreter belum siap.")
            return
        }

        // 1. Proses gambar input
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(image)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Siapkan buffer output
        // Model Anda memiliki 5 kelas, jadi output shape adalah [1, 5]
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 5), DataType.FLOAT32)

        // 3. Jalankan inferensi
        try {
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // 4. Proses hasil output
            val scores = outputBuffer.floatArray
            var maxScore = -1f
            var maxIndex = -1
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxIndex = i
                }
            }

            if (maxIndex != -1) {
                listener.onResult(labels[maxIndex], maxScore)
            }

        } catch (e: Exception) {
            listener.onError("Gagal saat melakukan klasifikasi: ${e.message}")
        }
    }

    fun close() {
        interpreter?.close()
    }

    interface ClassifierListener {
        fun onError(error: String)
        // Ubah listener untuk menerima hasil yang lebih sederhana
        fun onResult(label: String, score: Float)
    }
}