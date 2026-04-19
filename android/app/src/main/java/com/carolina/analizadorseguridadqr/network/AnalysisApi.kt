package com.carolina.analizadorseguridadqr.network

import com.carolina.analizadorseguridadqr.network.model.AnalyzeUrlRequest
import com.carolina.analizadorseguridadqr.network.model.AnalyzeUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalysisApi {
    @POST("api/v1/analyze")
    suspend fun analyzeUrl(
        @Body request: AnalyzeUrlRequest,
    ): AnalyzeUrlResponse
}
