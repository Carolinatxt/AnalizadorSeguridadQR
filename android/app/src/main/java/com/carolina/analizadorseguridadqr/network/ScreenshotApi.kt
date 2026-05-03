package com.carolina.analizadorseguridadqr.network

import com.carolina.analizadorseguridadqr.network.model.ScreenshotRequest
import com.carolina.analizadorseguridadqr.network.model.ScreenshotResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ScreenshotApi {
    @POST("api/v1/screenshot")
    suspend fun createScreenshot(
        @Body request: ScreenshotRequest,
    ): ScreenshotResponse
}
