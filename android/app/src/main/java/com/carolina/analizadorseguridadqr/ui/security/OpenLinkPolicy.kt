package com.carolina.analizadorseguridadqr.ui.security

import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel

enum class OpenLinkPolicy {
    SafeDirectOpen,
    ConfirmRiskyOpen,
    ConfirmDangerousOpen,
}

fun resolveOpenLinkPolicy(
    riskLevel: RiskLevel,
    analysisStatus: AnalysisStatus,
): OpenLinkPolicy {
    return when {
        riskLevel == RiskLevel.DANGEROUS -> OpenLinkPolicy.ConfirmDangerousOpen
        riskLevel == RiskLevel.SAFE && analysisStatus == AnalysisStatus.COMPLETE -> OpenLinkPolicy.SafeDirectOpen
        else -> OpenLinkPolicy.ConfirmRiskyOpen
    }
}
