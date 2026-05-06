package com.carolina.analizadorseguridadqr.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class QrPalette(
    val greenDark: Color,
    val greenLight: Color,
    val greenSoft: Color,
    val background: Color,
    val cardBackground: Color,
    val bottomBarBackground: Color,
    val bottomBarSelected: Color,
    val secondaryButton: Color,
    val outline: Color,
    val iconMuted: Color,
    val suspicious: Color,
    val suspiciousSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
)

private val LightQrPalette = QrPalette(
    greenDark = Color(0xFF0E7A2A),
    greenLight = Color(0xFF7DDB7B),
    greenSoft = Color(0xFFD4F3CF),
    background = Color(0xFFF5F7F6),
    cardBackground = Color(0xFFFFFFFF),
    bottomBarBackground = Color(0xFFF0F2F1),
    bottomBarSelected = Color(0xFFD9F1DF),
    secondaryButton = Color(0xFFD8DDE0),
    outline = Color(0xFFE2E5E8),
    iconMuted = Color(0xFFBCC3C8),
    suspicious = Color(0xFFC77700),
    suspiciousSoft = Color(0xFFFFF0DA),
    danger = Color(0xFFB3261E),
    dangerSoft = Color(0xFFFCE8E6),
    textPrimary = Color(0xFF31373B),
    textSecondary = Color(0xFF666D72),
)

private val DarkQrPalette = QrPalette(
    greenDark = Color(0xFF86E59A),
    greenLight = Color(0xFFC0F7C8),
    greenSoft = Color(0xFF1D3825),
    background = Color(0xFF0F1512),
    cardBackground = Color(0xFF17201B),
    bottomBarBackground = Color(0xFF131A16),
    bottomBarSelected = Color(0xFF203B28),
    secondaryButton = Color(0xFF2A3430),
    outline = Color(0xFF334039),
    iconMuted = Color(0xFF8E9D94),
    suspicious = Color(0xFFF2B44F),
    suspiciousSoft = Color(0xFF3F3014),
    danger = Color(0xFFFF8B84),
    dangerSoft = Color(0xFF472422),
    textPrimary = Color(0xFFE7F1EA),
    textSecondary = Color(0xFFB2C0B7),
)

private object QrPaletteHolder {
    var current: QrPalette = LightQrPalette
}

internal fun qrPaletteFor(darkTheme: Boolean): QrPalette = if (darkTheme) DarkQrPalette else LightQrPalette

internal fun applyQrPalette(palette: QrPalette) {
    QrPaletteHolder.current = palette
}

val QrGreenDark: Color
    get() = QrPaletteHolder.current.greenDark

val QrGreenLight: Color
    get() = QrPaletteHolder.current.greenLight

val QrGreenSoft: Color
    get() = QrPaletteHolder.current.greenSoft

val QrBackground: Color
    get() = QrPaletteHolder.current.background

val QrCardBackground: Color
    get() = QrPaletteHolder.current.cardBackground

val QrBottomBarBackground: Color
    get() = QrPaletteHolder.current.bottomBarBackground

val QrBottomBarSelected: Color
    get() = QrPaletteHolder.current.bottomBarSelected

val QrSecondaryButton: Color
    get() = QrPaletteHolder.current.secondaryButton

val QrOutline: Color
    get() = QrPaletteHolder.current.outline

val QrIconMuted: Color
    get() = QrPaletteHolder.current.iconMuted

val QrSuspicious: Color
    get() = QrPaletteHolder.current.suspicious

val QrSuspiciousSoft: Color
    get() = QrPaletteHolder.current.suspiciousSoft

val QrDanger: Color
    get() = QrPaletteHolder.current.danger

val QrDangerSoft: Color
    get() = QrPaletteHolder.current.dangerSoft

val QrTextPrimary: Color
    get() = QrPaletteHolder.current.textPrimary

val QrTextSecondary: Color
    get() = QrPaletteHolder.current.textSecondary
