package com.carolina.analizadorseguridadqr.ui.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

fun ThemeMode.resolveDarkTheme(systemDarkTheme: Boolean): Boolean {
    return when (this) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

fun ThemeMode.toDisplayLabel(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }
}

fun ThemeMode.toDisplayDescription(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "Sigue la configuracion actual del dispositivo."
        ThemeMode.LIGHT -> "Mantiene la app siempre en modo claro."
        ThemeMode.DARK -> "Mantiene la app siempre en modo oscuro."
    }
}
