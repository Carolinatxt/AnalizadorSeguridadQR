package com.carolina.analizadorseguridadqr.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
                text = "Detalle del analisis",
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

                DetailLabel("Estado del analisis")
                DetailValue(analysisStatusLabel(item.analysisStatus))
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("Resumen")
                DetailValue(
                    text = item.summary,
                    maxLines = 8,
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailLabel("Motivos")
                if (item.reasons.isEmpty()) {
                    DetailValue("Sin motivos adicionales.")
                } else {
                    item.reasons.forEach { reason ->
                        Text(
                            text = "- $reason",
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
    val isSafeSnapshot = openLinkPolicy == OpenLinkPolicy.SafeDirectOpen
    val isAnalysisIncompleteOrUncertain = analysisStatus == AnalysisStatus.PARTIAL ||
        analysisStatus == AnalysisStatus.UNAVAILABLE ||
        analysisStatus == AnalysisStatus.UNKNOWN
    val title = when {
        isDangerous -> "Enlace peligroso"
        isSafeSnapshot -> "Confirmar apertura"
        else -> "Antes de continuar"
    }
    val message = if (isDangerous) {
        "Este enlace ha sido relacionado con una amenaza grave. Abrirlo puede ponerte en riesgo. " +
            "Si continuas, se abrira fuera de la app en el navegador del dispositivo."
    } else if (isSafeSnapshot) {
        "Este resultado de seguridad corresponde a un momento anterior y puede haber cambiado. " +
            "Si continuas, se abrira fuera de la app en el navegador del dispositivo."
    } else {
        if (isAnalysisIncompleteOrUncertain) {
            "Este enlace no se ha clasificado como seguro o el analisis no pudo completarse del todo. " +
                "Si continuas, se abrira fuera de la app en el navegador del dispositivo."
        } else {
            "Este enlace no se ha clasificado como seguro. " +
                "Si continuas, se abrira fuera de la app en el navegador del dispositivo."
        }
    }
    val confirmText = when {
        isDangerous -> "Entiendo el riesgo"
        isSafeSnapshot -> "Abrir enlace"
        else -> "Abrir de todas formas"
    }

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
private fun DetailValue(
    text: String,
    maxLines: Int = 4,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
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
