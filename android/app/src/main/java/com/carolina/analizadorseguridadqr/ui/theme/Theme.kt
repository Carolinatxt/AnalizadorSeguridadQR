package com.carolina.analizadorseguridadqr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = QrGreenLight,
    secondary = QrSecondaryButton,
    tertiary = QrGreenSoft,
    background = QrBackground,
    surface = QrCardBackground,
    onPrimary = QrTextPrimary,
    onSecondary = QrTextPrimary,
    onBackground = QrTextPrimary,
    onSurface = QrTextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = QrGreenDark,
    secondary = QrSecondaryButton,
    tertiary = QrGreenSoft,
    background = QrBackground,
    surface = QrCardBackground,
    onPrimary = QrCardBackground,
    onSecondary = QrTextPrimary,
    onBackground = QrTextPrimary,
    onSurface = QrTextPrimary,
)

@Composable
fun AnalizadorSeguridadQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Se desactiva por defecto para respetar los colores del prototipo.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
