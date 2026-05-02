package com.carolina.analizadorseguridadqr.data.repository

import android.net.Uri
import android.util.Log
import com.carolina.analizadorseguridadqr.data.local.history.ScanHistoryDao
import com.carolina.analizadorseguridadqr.data.local.history.ScanHistoryEntity
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class ScanHistoryRepository(
    private val dao: ScanHistoryDao,
) {
    fun observeRecentScans(): Flow<List<ScanHistoryEntity>> {
        return dao.observeRecentScans()
    }

    suspend fun saveAnalysisResult(
        url: String,
        riskLevel: RiskLevel,
        analysisStatus: AnalysisStatus,
        summary: String,
        reasons: List<String>,
    ) {
        try {
            val safeSummary = sanitizeSummary(summary)
            val safeReasons = sanitizeReasons(reasons)
            val entity = ScanHistoryEntity(
                url = url,
                displayDomain = extractDisplayDomain(url),
                // Locale.ROOT evita cambios inesperados por idioma del dispositivo.
                riskLevel = riskLevel.name.lowercase(Locale.ROOT),
                analysisStatus = analysisStatus.name.lowercase(Locale.ROOT),
                summary = safeSummary,
                reasons = safeReasons,
                scannedAt = System.currentTimeMillis(),
            )
            dao.insertAndKeepOnlyLast(
                scan = entity,
                maxRows = MAX_HISTORY_ITEMS,
            )

            // No registrar nunca la URL completa: puede contener tokens o sesiones.
            Log.d(TAG, "Analisis guardado en historial local")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // El historial es secundario: no debe romper el flujo principal.
            Log.w(TAG, "No se pudo guardar el analisis en historial local", exception)
        }
    }

    suspend fun clearHistory() {
        try {
            dao.clearAll()
            Log.d(TAG, "Historial local borrado")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(TAG, "No se pudo borrar el historial local", exception)
        }
    }

    private fun extractDisplayDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host ?: "desconocido"
        } catch (_: Exception) {
            "desconocido"
        }
    }

    private fun sanitizeSummary(value: String): String {
        val cleanValue = value.trim()
        if (cleanValue.isBlank()) return DEFAULT_SUMMARY_FALLBACK
        return cleanValue.take(MAX_SUMMARY_LENGTH)
    }

    private fun sanitizeReasons(values: List<String>): List<String> {
        val normalized = values
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.take(MAX_REASON_LENGTH) }
            .distinct()
            .take(MAX_REASONS)
            .toList()

        return normalized
    }

    companion object {
        private const val TAG = "ScanHistoryRepository"
        private const val MAX_HISTORY_ITEMS = 100
        private const val MAX_SUMMARY_LENGTH = 280
        private const val MAX_REASON_LENGTH = 120
        private const val MAX_REASONS = 4
        private const val DEFAULT_SUMMARY_FALLBACK = "Sin resumen disponible."
    }
}
