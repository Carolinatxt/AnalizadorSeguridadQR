package com.carolina.analizadorseguridadqr.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
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
    onReanalyzeLink: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 24.dp)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Detalle del análisis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // En pantallas pequeñas, el contenido central se adapta
                        // y hace scroll sin sacar los botones fuera del viewport.
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    DetailLabel("Dominio")
                    DetailValue(item.displayDomain)
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailLabel("URL")
                    Text(
                        text = "La URL completa puede contener información sensible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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

                    Spacer(modifier = Modifier.height(12.dp))
                    ReanalyzeInfoBox(onReanalyzeLink = onReanalyzeLink)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        onClick = onDismiss,
                    ) {
                        Text("Cerrar")
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        onClick = onRequestOpenLink,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = "Abrir enlace",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryOpenLinkConfirmationDialog(
    openLinkPolicy: OpenLinkPolicy,
    riskLevel: RiskLevel,
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
        isSafeSnapshot -> "Abrir enlace guardado"
        else -> "Antes de continuar"
    }
    val message = if (isDangerous) {
        "Este enlace fue clasificado como peligroso en un análisis anterior. " +
            "Abrirlo puede ponerte en riesgo. Si continúas, se abrirá fuera de la app " +
            "en el navegador del dispositivo."
    } else if (isSafeSnapshot) {
        "Este enlace fue clasificado como seguro en un análisis anterior, " +
            "pero su estado podría haber cambiado desde entonces. Si continúas, " +
            "se abrirá fuera de la app en el navegador del dispositivo."
    } else {
        if (isAnalysisIncompleteOrUncertain) {
            "Este análisis anterior no pudo completarse del todo o su estado es incierto. " +
                "No hay base suficiente para considerar el enlace seguro. Si continúas, " +
                "se abrirá fuera de la app en el navegador del dispositivo."
        } else if (riskLevel == RiskLevel.SUSPICIOUS) {
            "Este enlace fue clasificado como sospechoso en un análisis anterior. " +
                "No hay base suficiente para considerarlo seguro. Si continúas, " +
                "se abrirá fuera de la app en el navegador del dispositivo."
        } else {
            "Este enlace no se ha clasificado como seguro. Si continúas, " +
                "se abrirá fuera de la app en el navegador del dispositivo."
        }
    }
    val confirmText = when {
        isDangerous -> "Entiendo el riesgo"
        else -> "Abrir de todos modos"
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
private fun ReanalyzeInfoBox(
    onReanalyzeLink: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Este resultado corresponde a un análisis anterior.\n" +
                    "El estado del enlace podría haber cambiado desde entonces.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "↻ Reanalizar ahora",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .semantics { role = Role.Button }
                    // La UI solo reenvía el callback existente.
                    // MainActivity conserva el flujo de cierre, cambio de pestaña y análisis.
                    .clickable(onClick = onReanalyzeLink)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
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
