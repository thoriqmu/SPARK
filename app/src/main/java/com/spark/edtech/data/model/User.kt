package com.spark.edtech.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val redeemCode: String? = null,
    val bio: String = ""
) : Parcelable