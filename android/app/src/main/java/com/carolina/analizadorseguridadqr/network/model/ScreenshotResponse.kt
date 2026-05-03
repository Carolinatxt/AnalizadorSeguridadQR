package com.carolina.analizadorseguridadqr.network.model

import com.google.gson.annotations.SerializedName

data class ScreenshotResponse(
    @SerializedName("available")
    val available: Boolean,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("message")
    val message: String,
)
