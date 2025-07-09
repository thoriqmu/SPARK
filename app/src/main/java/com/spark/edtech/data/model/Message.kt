package com.spark.edtech.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val messageId: String = "",
    val senderUid: String = "",
    val type: String = "text", // "text", "image", "audio"
    val content: String = "",
    val timestamp: Long = 0L,
    val replyTo: String? = null
) : Parcelable