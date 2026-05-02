package com.carolina.analizadorseguridadqr.ui.history

import com.carolina.analizadorseguridadqr.data.local.history.ScanHistoryEntity
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ScanHistoryEntity.toHistoryUiItem(): HistoryUiItem {
    val safeDisplayDomain = sanitizeDisplayDomain(displayDomain)

    return HistoryUiItem(
        id = id,
        url = url,
        title = buildHistoryTitle(safeDisplayDomain),
        displayDomain = safeDisplayDomain,
        dateText = formatHistoryDate(scannedAt),
        riskLevel = mapRiskLevel(riskLevel),
        analysisStatus = mapAnalysisStatus(analysisStatus),
        summary = sanitizeSummary(summary),
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

private fun sanitizeDisplayDomain(value: String): String {
    val cleanValue = value.trim().lowercase(Locale.ROOT)
    if (cleanValue.isBlank()) return "desconocido"
    return cleanValue.take(MAX_DISPLAY_DOMAIN_LENGTH)
}

private fun sanitizeSummary(value: String): String {
    val cleanValue = value.trim()
    if (cleanValue.isBlank()) return DEFAULT_SUMMARY_FALLBACK
    return cleanValue.take(MAX_SUMMARY_LENGTH)
}

private fun mapRiskLevel(value: String): RiskLevel {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "safe" -> RiskLevel.SAFE
        "suspicious" -> RiskLevel.SUSPICIOUS
        "dangerous" -> RiskLevel.DANGEROUS
        else -> RiskLevel.UNKNOWN
    }
}

private fun mapAnalysisStatus(value: String): AnalysisStatus {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "complete" -> AnalysisStatus.COMPLETE
        "partial" -> AnalysisStatus.PARTIAL
        "unavailable" -> AnalysisStatus.UNAVAILABLE
        else -> AnalysisStatus.UNKNOWN
    }
}

private const val MAX_DISPLAY_DOMAIN_LENGTH = 120
private const val MAX_SUMMARY_LENGTH = 280
private const val DEFAULT_SUMMARY_FALLBACK = "Sin resumen disponible."
