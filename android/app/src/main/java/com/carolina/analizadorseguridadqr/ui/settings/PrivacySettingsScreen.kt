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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
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
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary

@Composable
fun PrivacySettingsScreen(
    onBackClick: () -> Unit,
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
                title = "Privacidad",
                onBackClick = onBackClick,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Privacidad",
                style = MaterialTheme.typography.headlineMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Explicamos de forma clara que ocurre durante cada analisis y que datos permanecen en tu dispositivo.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsInfoCard(
                title = "Durante el analisis",
                body = "Durante el analisis, las URLs escaneadas se envian al servidor de analisis de la aplicacion y a proveedores externos de seguridad, como Google Web Risk e IPQualityScore, para comprobar si existen amenazas conocidas.",
                icon = Icons.Outlined.Security,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Proveedores externos",
                body = "Los proveedores externos pueden registrar o tratar las URLs analizadas segun sus propias politicas de privacidad.",
                icon = Icons.Outlined.Language,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Almacenamiento local",
                body = "El historial de resultados se guarda unicamente en este dispositivo. No se sincroniza en la nube ni se almacena como historial en el servidor de la aplicacion.",
                icon = Icons.Outlined.CloudOff,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Vista previa avanzada",
                body = "Si solicitas una vista previa visual, la URL analizada se enviara a SnapRender, un proveedor externo de capturas web, para generar una imagen de la pagina.\n\nPara mostrar la captura, la app carga una URL firmada generada por SnapRender. Esto implica que SnapRender puede recibir la URL tanto al generar la firma desde el backend como al renderizar la imagen solicitada desde el dispositivo.\n\nEsta funcion es opcional, solo se ejecuta cuando pulsas explicitamente 'Generar vista previa' y no forma parte de la decision principal de seguridad. La captura no se guarda en el historial local.\n\nPara reducir el consumo de cuota gratuita, SnapRender puede devolver una captura cacheada durante hasta 24 horas, por lo que la imagen puede no reflejar el estado actual exacto de la pagina.",
                icon = Icons.Outlined.Language,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Prototipo academico",
                body = "Esta aplicacion es un prototipo academico. No solicita datos personales de forma intencionada ni sincroniza el historial con servidores externos. Los proveedores externos pueden tratar las URLs analizadas segun sus propias politicas.",
                icon = Icons.Outlined.Info,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacySettingsScreenPreview() {
    AnalizadorSeguridadQRTheme {
        PrivacySettingsScreen(
            onBackClick = {},
            onScanTabClick = {},
            onHistoryTabClick = {},
            onSettingsTabClick = {},
        )
    }
}
