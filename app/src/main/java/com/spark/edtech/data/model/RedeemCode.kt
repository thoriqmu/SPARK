package com.spark.edtech.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.firebase.database.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RedeemCode(
    @PropertyName("code") @JsonProperty("code")
    val code: String = "",

    @PropertyName("isUsed") @JsonProperty("isUsed")
    var isUsed: Boolean = false,

    @PropertyName("createdAt") @JsonProperty("createdAt")
    val createdAt: String = ""
) : Parcelable