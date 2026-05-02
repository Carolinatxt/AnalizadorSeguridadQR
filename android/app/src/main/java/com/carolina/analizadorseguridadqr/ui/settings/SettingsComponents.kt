package com.carolina.analizadorseguridadqr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarSelected
import com.carolina.analizadorseguridadqr.ui.theme.QrCardBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenDark
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrIconMuted
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary

/**
 * Barra superior reutilizable para Ajustes.
 *
 * `actionContentDescription` es obligatoria cuando `actionIcon` no es null.
 */
@Composable
fun SettingsTopBar(
    title: String,
    onBackClick: () -> Unit,
    showBackButton: Boolean = true,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val resolvedActionContentDescription = if (actionIcon != null) {
        actionContentDescription ?: error(
            "actionContentDescription es obligatorio cuando actionIcon no es null",
        )
    } else {
        null
    }
    val resolvedActionClick = if (actionIcon != null) {
        onActionClick ?: error("onActionClick es obligatorio cuando actionIcon no es null")
    } else {
        null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = QrTextSecondary,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = QrTextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (actionIcon != null && resolvedActionClick != null) {
            IconButton(onClick = resolvedActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = resolvedActionContentDescription,
                    tint = QrTextSecondary,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = QrGreenDark,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SettingsOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .applyOptionalTestTag(testTag)
            .clip(RoundedCornerShape(24.dp))
            .background(QrCardBackground)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsLeadingIcon(icon = icon)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = QrTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QrTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = QrIconMuted,
            )
        }
    }
}

@Composable
fun SettingsInfoCard(
    title: String,
    body: String,
    icon: ImageVector,
    titleColor: Color = QrTextPrimary,
    bodyColor: Color = QrTextSecondary,
    iconTint: Color = QrGreenDark,
    iconBackground: Color = QrGreenSoft,
    iconContentDescription: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(QrCardBackground)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SettingsLeadingIcon(
                icon = icon,
                iconTint = iconTint,
                iconBackground = iconBackground,
                contentDescription = iconContentDescription,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                )
            }
        }
    }
}

@Composable
fun SettingsBottomBar(
    onScanTabClick: () -> Unit,
    onHistoryTabClick: () -> Unit,
    onSettingsTabClick: () -> Unit,
) {
    // Este componente es exclusivo de la sección Ajustes.
    // La pestaña Ajustes siempre aparece como seleccionada.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(QrBottomBarBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsBottomBarItem(
            label = "ESCANEAR",
            icon = Icons.Outlined.QrCode2,
            selected = false,
            onClick = onScanTabClick,
        )
        SettingsBottomBarItem(
            label = "HISTORIAL",
            icon = Icons.Outlined.History,
            selected = false,
            onClick = onHistoryTabClick,
        )
        SettingsBottomBarItem(
            label = "AJUSTES",
            icon = Icons.Outlined.Settings,
            selected = true,
            onClick = onSettingsTabClick,
        )
    }
}

@Composable
private fun SettingsLeadingIcon(
    icon: ImageVector,
    iconTint: Color = QrGreenDark,
    iconBackground: Color = QrGreenSoft,
    contentDescription: String? = null,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(iconBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
        )
    }
}

private fun Modifier.applyOptionalTestTag(testTag: String?): Modifier {
    return if (testTag.isNullOrBlank()) this else this.testTag(testTag)
}

@Composable
private fun RowScope.SettingsBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val itemTint = if (selected) QrGreenDark else QrTextSecondary
    val itemBackground = if (selected) QrBottomBarSelected else Color.Transparent

    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(itemBackground)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = itemTint,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = itemTint,
            )
        }
    }
}
