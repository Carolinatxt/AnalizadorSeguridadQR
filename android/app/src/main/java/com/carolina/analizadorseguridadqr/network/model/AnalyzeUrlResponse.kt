package com.carolina.analizadorseguridadqr.network.model

import com.google.gson.annotations.SerializedName

data class AnalyzeUrlResponse(
    @SerializedName("risk_level")
    val riskLevel: String,
    @SerializedName("analysis_status")
    val analysisStatus: String,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("reasons")
    val reasons: List<String> = emptyList(),
)
