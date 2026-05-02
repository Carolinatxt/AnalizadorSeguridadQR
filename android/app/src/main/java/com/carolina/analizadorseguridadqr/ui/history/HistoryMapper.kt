package com.carolina.analizadorseguridadqr.ui.history

import com.carolina.analizadorseguridadqr.data.local.history.ScanHistoryEntity
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ScanHistoryEntity.toHistoryUiItem(): HistoryUiItem {
    return HistoryUiItem(
        id = id,
        url = url,
        title = buildHistoryTitle(displayDomain),
        displayDomain = displayDomain,
        dateText = formatHistoryDate(scannedAt),
        riskLevel = mapRiskLevel(riskLevel),
        analysisStatus = mapAnalysisStatus(analysisStatus),
        summary = summary,
        reasons = reasons,
    )
}

private fun buildHistoryTitle(displayDomain: String): String {
    return if (displayDomain.length > 24) {
        displayDomain.take(21) + "..."
    } else {
        displayDomain
    }
}

private fun formatHistoryDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy  •  HH:mm", Locale.forLanguageTag("es-ES"))
    return formatter.format(Date(timestamp))
}

private fun mapRiskLevel(value: String): RiskLevel {
    return when (value.lowercase(Locale.ROOT)) {
        "safe" -> RiskLevel.SAFE
        "suspicious" -> RiskLevel.SUSPICIOUS
        "dangerous" -> RiskLevel.DANGEROUS
        else -> RiskLevel.UNKNOWN
    }
}

private fun mapAnalysisStatus(value: String): AnalysisStatus {
    return when (value.lowercase(Locale.ROOT)) {
        "complete" -> AnalysisStatus.COMPLETE
        "partial" -> AnalysisStatus.PARTIAL
        "unavailable" -> AnalysisStatus.UNAVAILABLE
        else -> AnalysisStatus.UNKNOWN
    }
}
