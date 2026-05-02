package com.carolina.analizadorseguridadqr.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.ui.theme.QrBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrDanger
import com.carolina.analizadorseguridadqr.ui.theme.QrDangerSoft
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
                text = "Consulta que informacion se conserva localmente y decide cuando eliminarla.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsInfoCard(
                title = "Datos guardados",
                body = "Se conservan hasta 100 analisis recientes en este dispositivo: URL completa, dominio, nivel de riesgo, estado del analisis, resumen, motivos y fecha.\n\nLa URL completa se guarda para permitir el reanalisis. Si la URL contiene parametros, identificadores o tokens, estos quedan almacenados localmente en tu dispositivo.",
                icon = Icons.Outlined.History,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Borrar todos los datos",
                body = "Esta accion eliminara permanentemente los analisis guardados en este dispositivo.",
                icon = Icons.Outlined.Delete,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // El borrado real queda fuera de esta pantalla.
            // Solo avisamos al estado superior para mostrar confirmacion.
            Button(
                onClick = onClearHistoryClick,
                enabled = hasHistory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QrDanger,
                    contentColor = QrBackground,
                    disabledContainerColor = QrDangerSoft,
                    disabledContentColor = QrTextSecondary,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                modifier = Modifier,
            ) {
                Text(text = "Borrar historial")
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasHistory) {
                Text(
                    text = "Puedes borrar el historial en cualquier momento desde este apartado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QrTextSecondary,
                )
            } else {
                Text(
                    text = "No hay analisis guardados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QrTextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Sin sincronizacion",
                body = "El historial no se sincroniza en la nube ni se almacena como historial en el servidor de la aplicacion.",
                icon = Icons.Outlined.CloudOff,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
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
