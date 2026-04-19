package com.carolina.analizadorseguridadqr.network

import com.carolina.analizadorseguridadqr.network.model.AnalyzeUrlRequest
import com.carolina.analizadorseguridadqr.network.model.AnalyzeUrlResponse

// Capa minima de red para mantener el ViewModel limpio.
class AnalysisService(
    private val api: AnalysisApi = AnalysisApiClient.api,
) {
    suspend fun analyzeUrl(url: String): AnalyzeUrlResponse {
        return api.analyzeUrl(AnalyzeUrlRequest(url = url))
    }
}
