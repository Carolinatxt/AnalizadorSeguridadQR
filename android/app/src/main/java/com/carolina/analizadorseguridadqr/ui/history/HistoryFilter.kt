package com.carolina.analizadorseguridadqr.ui.history

import com.carolina.analizadorseguridadqr.ui.state.RiskLevel

enum class HistoryFilter {
    ALL,
    SAFE,
    SUSPICIOUS,
    DANGEROUS,
}

fun List<HistoryUiItem>.filterBy(filter: HistoryFilter): List<HistoryUiItem> {
    return when (filter) {
        HistoryFilter.ALL -> this
        HistoryFilter.SAFE -> filter { it.riskLevel == RiskLevel.SAFE }
        HistoryFilter.SUSPICIOUS -> filter { it.riskLevel == RiskLevel.SUSPICIOUS }
        HistoryFilter.DANGEROUS -> filter { it.riskLevel == RiskLevel.DANGEROUS }
    }
}
