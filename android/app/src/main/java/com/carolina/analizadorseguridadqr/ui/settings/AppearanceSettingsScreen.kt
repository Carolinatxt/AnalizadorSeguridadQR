package com.carolina.analizadorseguridadqr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.ui.theme.QrBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrCardBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenDark
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrIconMuted
import com.carolina.analizadorseguridadqr.ui.theme.QrOutline
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary
import com.carolina.analizadorseguridadqr.ui.theme.ThemeMode
import com.carolina.analizadorseguridadqr.ui.theme.toDisplayDescription
import com.carolina.analizadorseguridadqr.ui.theme.toDisplayLabel

@Composable
fun AppearanceSettingsScreen(
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
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
                title = "Apariencia",
                onBackClick = onBackClick,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tema de la aplicacion",
                style = MaterialTheme.typography.headlineMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Elige como quieres ver la interfaz. La opcion Sistema sigue la configuracion visual de Android.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsInfoCard(
                title = "Tema actual",
                body = selectedThemeMode.toDisplayLabel(),
                icon = Icons.Outlined.Palette,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MODOS DISPONIBLES",
                style = MaterialTheme.typography.labelLarge,
                color = QrGreenDark,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            ThemeModeSelectorCard(
                selectedThemeMode = selectedThemeMode,
                onThemeModeSelected = onThemeModeSelected,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoCard(
                title = "Aplicacion inmediata",
                body = "El cambio se aplica al instante y queda guardado localmente en este dispositivo.",
                icon = Icons.Outlined.SettingsSuggest,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ThemeModeSelectorCard(
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(QrCardBackground)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .selectableGroup(),
    ) {
        ThemeMode.entries.forEachIndexed { index, themeMode ->
            ThemeModeOptionRow(
                themeMode = themeMode,
                selected = themeMode == selectedThemeMode,
                onClick = { onThemeModeSelected(themeMode) },
            )

            if (index < ThemeMode.entries.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(QrOutline),
                )
            }
        }
    }
}

@Composable
private fun ThemeModeOptionRow(
    themeMode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .semantics {
                contentDescription = buildString {
                    append(themeMode.toDisplayLabel())
                    append(". ")
                    append(if (selected) "Seleccionado" else "No seleccionado")
                }
            }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeModeLeadingIcon(themeMode = themeMode)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = themeMode.toDisplayLabel(),
                style = MaterialTheme.typography.titleMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = themeMode.toDisplayDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = QrTextSecondary,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = QrGreenDark,
                unselectedColor = QrIconMuted,
            ),
        )
    }
}

@Composable
private fun ThemeModeLeadingIcon(themeMode: ThemeMode) {
    val icon: ImageVector = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Outlined.Palette
        ThemeMode.LIGHT -> Icons.Outlined.LightMode
        ThemeMode.DARK -> Icons.Outlined.DarkMode
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(QrGreenSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QrGreenDark,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearanceSettingsScreenPreview() {
    AnalizadorSeguridadQRTheme(darkTheme = false) {
        AppearanceSettingsScreen(
            selectedThemeMode = ThemeMode.SYSTEM,
            onThemeModeSelected = {},
            onBackClick = {},
            onScanTabClick = {},
            onHistoryTabClick = {},
            onSettingsTabClick = {},
        )
    }
}
