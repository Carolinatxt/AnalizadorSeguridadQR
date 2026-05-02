package com.carolina.analizadorseguridadqr.data.local.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Se guarda para poder revisar o abrir el análisis más tarde.
    // Importante: no registrar nunca la URL completa en logs.
    val url: String,
    val displayDomain: String,
    val riskLevel: String,
    val analysisStatus: String,
    val summary: String,
    val reasons: List<String>,
    val scannedAt: Long = System.currentTimeMillis(),
)
