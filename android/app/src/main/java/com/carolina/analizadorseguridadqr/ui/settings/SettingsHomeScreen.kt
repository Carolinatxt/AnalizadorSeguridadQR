package com.carolina.analizadorseguridadqr.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.ui.theme.QrBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary
import com.carolina.analizadorseguridadqr.ui.theme.ThemeMode
import com.carolina.analizadorseguridadqr.ui.theme.toDisplayLabel

@Composable
fun SettingsHomeScreen(
    currentThemeMode: ThemeMode,
    onBackClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onHistoryShortcutClick: () -> Unit,
    onHistorySettingsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
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
                title = "Ajustes",
                onBackClick = onBackClick,
                showBackButton = false,
                actionIcon = Icons.Outlined.History,
                actionContentDescription = "Abrir historial",
                onActionClick = onHistoryShortcutClick,
            )
            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionLabel(text = "CONFIGURACION GENERAL")
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "QR Scanner Security",
                style = MaterialTheme.typography.headlineMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gestiona tu seguridad y personaliza tu experiencia digital.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(26.dp))

            SettingsOptionCard(
                title = "Apariencia",
                subtitle = "Tema actual: ${currentThemeMode.toDisplayLabel()}",
                icon = Icons.Outlined.Palette,
                onClick = onAppearanceClick,
                testTag = "settings_appearance_card",
            )
            Spacer(modifier = Modifier.height(14.dp))
            SettingsOptionCard(
                title = "Ajustes Historial",
                subtitle = "Registro local y borrado de analisis",
                icon = Icons.Outlined.History,
                onClick = onHistorySettingsClick,
                testTag = "settings_history_card",
            )
            Spacer(modifier = Modifier.height(14.dp))
            SettingsOptionCard(
                title = "Privacidad",
                subtitle = "Uso de datos y almacenamiento local",
                icon = Icons.Outlined.Security,
                onClick = onPrivacyClick,
                testTag = "settings_privacy_card",
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsHomeScreenPreview() {
    AnalizadorSeguridadQRTheme(darkTheme = false) {
        SettingsHomeScreen(
            currentThemeMode = ThemeMode.SYSTEM,
            onBackClick = {},
            onAppearanceClick = {},
            onHistoryShortcutClick = {},
            onHistorySettingsClick = {},
            onPrivacyClick = {},
            onScanTabClick = {},
            onHistoryTabClick = {},
            onSettingsTabClick = {},
        )
    }
}
