package com.carolina.analizadorseguridadqr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private fun lightColorSchemeFor(palette: QrPalette) = lightColorScheme(
    primary = palette.greenDark,
    secondary = palette.secondaryButton,
    tertiary = palette.greenSoft,
    background = palette.background,
    surface = palette.cardBackground,
    surfaceVariant = palette.bottomBarBackground,
    outline = palette.outline,
    error = palette.danger,
    onPrimary = palette.cardBackground,
    onSecondary = palette.textPrimary,
    onTertiary = palette.textPrimary,
    onBackground = palette.textPrimary,
    onSurface = palette.textPrimary,
    onSurfaceVariant = palette.textSecondary,
    onError = palette.cardBackground,
)

private fun darkColorSchemeFor(palette: QrPalette) = darkColorScheme(
    primary = palette.greenDark,
    secondary = palette.secondaryButton,
    tertiary = palette.greenSoft,
    background = palette.background,
    surface = palette.cardBackground,
    surfaceVariant = palette.bottomBarBackground,
    outline = palette.outline,
    error = palette.danger,
    onPrimary = palette.background,
    onSecondary = palette.textPrimary,
    onTertiary = palette.textPrimary,
    onBackground = palette.textPrimary,
    onSurface = palette.textPrimary,
    onSurfaceVariant = palette.textSecondary,
    onError = palette.background,
)

@Composable
fun AnalizadorSeguridadQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Se desactiva por defecto para respetar la identidad visual de la app.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = qrPaletteFor(darkTheme)
    applyQrPalette(palette)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorSchemeFor(palette)
        else -> lightColorSchemeFor(palette)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
