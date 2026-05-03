package com.carolina.analizadorseguridadqr.ui.security

import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel

sealed class ScreenshotPreviewPolicy {
    data object AllowDirect : ScreenshotPreviewPolicy()
    data object RequireConfirmation : ScreenshotPreviewPolicy()
    data object Blocked : ScreenshotPreviewPolicy()
}

fun resolveScreenshotPreviewPolicy(
    riskLevel: RiskLevel,
    analysisStatus: AnalysisStatus,
): ScreenshotPreviewPolicy {
    return when {
        riskLevel == RiskLevel.DANGEROUS -> ScreenshotPreviewPolicy.Blocked
        riskLevel == RiskLevel.UNKNOWN -> ScreenshotPreviewPolicy.Blocked
        riskLevel == RiskLevel.SAFE && analysisStatus == AnalysisStatus.COMPLETE -> {
            ScreenshotPreviewPolicy.AllowDirect
        }

        riskLevel == RiskLevel.SUSPICIOUS -> ScreenshotPreviewPolicy.RequireConfirmation
        riskLevel == RiskLevel.SAFE && analysisStatus == AnalysisStatus.PARTIAL -> {
            ScreenshotPreviewPolicy.RequireConfirmation
        }

        riskLevel == RiskLevel.SAFE && analysisStatus == AnalysisStatus.UNAVAILABLE -> {
            ScreenshotPreviewPolicy.RequireConfirmation
        }

        riskLevel == RiskLevel.SAFE && analysisStatus == AnalysisStatus.UNKNOWN -> {
            ScreenshotPreviewPolicy.RequireConfirmation
        }

        else -> ScreenshotPreviewPolicy.Blocked
    }
}
