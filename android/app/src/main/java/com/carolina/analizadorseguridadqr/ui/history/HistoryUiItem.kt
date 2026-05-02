package com.carolina.analizadorseguridadqr.ui.history

import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel

data class HistoryUiItem(
    val id: Long,
    val url: String,
    val title: String,
    val displayDomain: String,
    val dateText: String,
    val riskLevel: RiskLevel,
    val analysisStatus: AnalysisStatus,
    val summary: String,
    val reasons: List<String>,
)
