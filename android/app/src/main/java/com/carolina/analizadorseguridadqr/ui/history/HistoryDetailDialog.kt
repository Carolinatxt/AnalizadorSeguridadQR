package com.carolina.analizadorseguridadqr.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.security.OpenLinkPolicy
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel

@Composable
fun HistoryDetailDialog(
    item: HistoryUiItem,
    onDismiss: () -> Unit,
    onRequestOpenLink: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Detalle del análisis",
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                DetailLabel("Dominio")
                DetailValue(item.displayDomain)
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("URL")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                ) {
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("Nivel de riesgo")
                DetailValue(riskLevelLabel(item.riskLevel))
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("Estado del análisis")
                DetailValue(analysisStatusLabel(item.analysisStatus))
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("Resumen")
                DetailValue(item.summary)
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("Motivos")
                if (item.reasons.isEmpty()) {
                    DetailValue("Sin motivos adicionales.")
                } else {
                    item.reasons.forEach { reason ->
                        Text(
                            text = "• $reason",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRequestOpenLink) {
                Text("Abrir enlace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}

@Composable
fun HistoryOpenLinkConfirmationDialog(
    openLinkPolicy: OpenLinkPolicy,
    analysisStatus: AnalysisStatus,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isDangerous = openLinkPolicy == OpenLinkPolicy.ConfirmDangerousOpen
    val isAnalysisIncompleteOrUncertain = analysisStatus == AnalysisStatus.PARTIAL ||
        analysisStatus == AnalysisStatus.UNAVAILABLE ||
        analysisStatus == AnalysisStatus.UNKNOWN
    val title = if (isDangerous) "Enlace peligroso" else "Antes de continuar"
    val message = if (isDangerous) {
        "Este enlace ha sido relacionado con una amenaza grave. Abrirlo puede ponerte en riesgo. " +
            "Si continúas, se abrirá fuera de la app en el navegador del dispositivo."
    } else {
        if (isAnalysisIncompleteOrUncertain) {
            "Este enlace no se ha clasificado como seguro o el análisis no pudo completarse del todo. " +
                "Si continúas, se abrirá fuera de la app en el navegador del dispositivo."
        } else {
            "Este enlace no se ha clasificado como seguro. " +
                "Si continúas, se abrirá fuera de la app en el navegador del dispositivo."
        }
    }
    val confirmText = if (isDangerous) "Entiendo el riesgo" else "Abrir de todas formas"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DetailValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun riskLevelLabel(riskLevel: RiskLevel): String {
    return when (riskLevel) {
        RiskLevel.SAFE -> "Segura"
        RiskLevel.SUSPICIOUS -> "Sospechosa"
        RiskLevel.DANGEROUS -> "Peligrosa"
        RiskLevel.UNKNOWN -> "Desconocido"
    }
}

private fun analysisStatusLabel(status: AnalysisStatus): String {
    return when (status) {
        AnalysisStatus.COMPLETE -> "Completo"
        AnalysisStatus.PARTIAL -> "Parcial"
        AnalysisStatus.UNAVAILABLE -> "No disponible"
        AnalysisStatus.UNKNOWN -> "Desconocido"
    }
}
