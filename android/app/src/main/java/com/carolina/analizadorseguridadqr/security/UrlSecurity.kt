package com.carolina.analizadorseguridadqr.security

import android.net.Uri
import java.util.Locale

const val MAX_WEB_URL_LENGTH = 2048

enum class UnsupportedWebUrlReason {
    TOO_LONG,
    CONTROL_CHARS,
    USERINFO,
    PARSE_ERROR,
    UNSUPPORTED_WEB_TARGET,
}

fun isSupportedWebUrl(value: String): Boolean {
    return getUnsupportedWebUrlReason(value) == null
}

fun getUnsupportedWebUrlReason(value: String): UnsupportedWebUrlReason? {
    val normalizedValue = value.trim()

    if (normalizedValue.length > MAX_WEB_URL_LENGTH) {
        return UnsupportedWebUrlReason.TOO_LONG
    }

    if (normalizedValue.any { it.isISOControl() }) {
        return UnsupportedWebUrlReason.CONTROL_CHARS
    }

    val uri = try {
        Uri.parse(normalizedValue)
    } catch (_: Exception) {
        return UnsupportedWebUrlReason.PARSE_ERROR
    }

    if (!uri.userInfo.isNullOrBlank()) {
        return UnsupportedWebUrlReason.USERINFO
    }

    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    val host = uri.host
    return if ((scheme == "http" || scheme == "https") && !host.isNullOrBlank()) {
        null
    } else {
        UnsupportedWebUrlReason.UNSUPPORTED_WEB_TARGET
    }
}
