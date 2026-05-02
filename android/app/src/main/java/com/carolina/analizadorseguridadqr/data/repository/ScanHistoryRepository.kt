package com.carolina.analizadorseguridadqr.data.repository

import android.net.Uri
import android.util.Log
import com.carolina.analizadorseguridadqr.data.local.history.ScanHistoryDao
import com.carolina.analizadorseguridadqr.data.local.history.ScanHistoryEntity
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
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
            val entity = ScanHistoryEntity(
                url = url,
                displayDomain = extractDisplayDomain(url),
                // Locale.ROOT evita cambios inesperados por idioma del dispositivo.
                riskLevel = riskLevel.name.lowercase(Locale.ROOT),
                analysisStatus = analysisStatus.name.lowercase(Locale.ROOT),
                summary = summary,
                reasons = reasons,
                scannedAt = System.currentTimeMillis(),
            )
            dao.insert(entity)
            dao.keepOnlyLast(MAX_HISTORY_ITEMS)

            // No registrar nunca la URL completa: puede contener tokens o sesiones.
            Log.d(TAG, "Analisis guardado en historial local")
        } catch (exception: Exception) {
            // El historial es secundario: no debe romper el flujo principal.
            Log.w(TAG, "No se pudo guardar el analisis en historial local", exception)
        }
    }

    suspend fun clearHistory() {
        try {
            dao.clearAll()
            Log.d(TAG, "Historial local borrado")
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

    companion object {
        private const val TAG = "ScanHistoryRepository"
        private const val MAX_HISTORY_ITEMS = 100
    }
}
