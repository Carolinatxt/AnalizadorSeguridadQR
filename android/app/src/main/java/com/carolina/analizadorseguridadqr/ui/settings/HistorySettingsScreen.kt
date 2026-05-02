package com.carolina.analizadorseguridadqr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.ui.theme.QrBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrDanger
import com.carolina.analizadorseguridadqr.ui.theme.QrDangerSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrIconMuted
import com.carolina.analizadorseguridadqr.ui.theme.QrOutline
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspicious
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspiciousSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary

@Composable
fun HistorySettingsScreen(
    hasHistory: Boolean,
    onBackClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
    onScanTabClick: () -> Unit,
    onHistoryTabClick: () -> Unit,
    onSettingsTabClick: () -> Unit,
) {
    Scaffold(
        containerColor = QrBackground,
        bottomBar = {
            SettingsBottomBar(
                onScanTabClick = onScanTabClick,
                onHistoryTabClick = onHistoryTabClick,
                onSettingsTabClick = onSettingsTabClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsTopBar(
                title = "Ajustes Historial",
                onBackClick = onBackClick,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Privacidad de tus datos",
                style = MaterialTheme.typography.headlineMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Consulta qué información se conserva localmente y decide cuándo eliminarla.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsInfoCard(
                title = "Datos guardados",
                body = "Se conservan hasta 100 análisis recientes en este dispositivo: URL completa, dominio, nivel de riesgo, estado del análisis, resumen, motivos y fecha.",
                icon = Icons.Outlined.History,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "URL completa almacenada",
                body = "La URL completa se guarda para permitir el reanálisis. Si la URL contiene parámetros, identificadores o tokens, estos quedan almacenados localmente en tu dispositivo.",
                icon = Icons.Outlined.Info,
                titleColor = QrSuspicious,
                iconTint = QrSuspicious,
                iconBackground = QrSuspiciousSoft,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (hasHistory) {
                SettingsInfoCard(
                    title = "Borrar todos los datos",
                    body = "Esta acción eliminará permanentemente los análisis guardados en este dispositivo.",
                    icon = Icons.Outlined.Delete,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // El borrado real queda fuera de esta pantalla.
                // Solo avisamos al estado superior para mostrar confirmación.
                Button(
                    onClick = onClearHistoryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QrDanger,
                        contentColor = QrBackground,
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .testTag("settings_clear_history_button")
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(text = "Borrar historial")
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Puedes borrar el historial en cualquier momento desde este apartado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QrTextSecondary,
                )
            } else {
                HistorySettingsEmptyState()
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Sin sincronización",
                body = "El historial no se sincroniza en la nube ni se almacena como historial en el servidor de la aplicación.",
                icon = Icons.Outlined.CloudOff,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun HistorySettingsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(QrOutline),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = "Historial vacío",
                tint = QrIconMuted,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No hay análisis guardados.",
            style = MaterialTheme.typography.titleMedium,
            color = QrTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cuando analices un enlace, el resultado aparecerá aquí automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = QrTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorySettingsScreenPreviewWithData() {
    AnalizadorSeguridadQRTheme {
        HistorySettingsScreen(
            hasHistory = true,
            onBackClick = {},
            onClearHistoryClick = {},
            onScanTabClick = {},
            onHistoryTabClick = {},
            onSettingsTabClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorySettingsScreenPreviewEmpty() {
    AnalizadorSeguridadQRTheme {
        HistorySettingsScreen(
            hasHistory = false,
            onBackClick = {},
            onClearHistoryClick = {},
            onScanTabClick = {},
            onHistoryTabClick = {},
            onSettingsTabClick = {},
        )
    }
}
