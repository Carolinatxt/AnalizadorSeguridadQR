package com.carolina.analizadorseguridadqr.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarSelected
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenDark
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary

@Composable
fun AppBottomBar(
    selectedTab: AppTab,
    onScanClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(QrBottomBarBackground)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppBottomBarItem(
            modifier = Modifier.weight(1f),
            label = "ESCANEAR",
            icon = Icons.Outlined.QrCode2,
            selected = selectedTab == AppTab.SCAN,
            onClick = onScanClick,
        )
        AppBottomBarItem(
            modifier = Modifier.weight(1f),
            label = "HISTORIAL",
            icon = Icons.Outlined.History,
            selected = selectedTab == AppTab.HISTORY,
            onClick = onHistoryClick,
        )
        AppBottomBarItem(
            modifier = Modifier.weight(1f),
            label = "AJUSTES",
            icon = Icons.Outlined.Settings,
            selected = selectedTab == AppTab.SETTINGS,
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun AppBottomBarItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) QrBottomBarSelected else Color.Transparent
    val tint = if (selected) QrGreenDark else QrTextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
