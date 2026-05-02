package com.carolina.analizadorseguridadqr.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
import com.carolina.analizadorseguridadqr.ui.theme.QrBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarSelected
import com.carolina.analizadorseguridadqr.ui.theme.QrCardBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrDanger
import com.carolina.analizadorseguridadqr.ui.theme.QrDangerSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenDark
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrIconMuted
import com.carolina.analizadorseguridadqr.ui.theme.QrOutline
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspicious
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspiciousSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary

@Composable
fun HistoryScreen(
    historyItems: List<HistoryUiItem>,
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
    onBackClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
    onItemClick: (HistoryUiItem) -> Unit,
    onScanTabClick: () -> Unit,
    onSettingsTabClick: () -> Unit,
) {
    val filteredItems = historyItems.filterBy(selectedFilter)

    Scaffold(
        containerColor = QrBackground,
        bottomBar = {
            HistoryBottomBar(
                onScanTabClick = onScanTabClick,
                onSettingsTabClick = onSettingsTabClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
        ) {
            HistoryTopBar(
                onBackClick = onBackClick,
                onClearHistoryClick = onClearHistoryClick,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Escaneos recientes",
                style = MaterialTheme.typography.headlineMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registro detallado de tus últimos análisis de seguridad.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(18.dp))

            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (historyItems.isEmpty()) {
                HistoryEmptyState(
                    title = "Aún no hay escaneos guardados.",
                    message = "Cuando analices un código QR, aparecerá aquí.",
                )
            } else if (filteredItems.isEmpty()) {
                HistoryEmptyState(
                    title = "No hay escaneos con este filtro.",
                    message = "Prueba con otra categoría.",
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // La key estable evita recomposiciones incorrectas al borrar, filtrar o insertar elementos.
                    items(
                        items = filteredItems,
                        key = { it.id },
                    ) { item ->
                        HistoryItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(
    onBackClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = QrTextSecondary,
            )
        }
        Text(
            text = "Historial",
            style = MaterialTheme.typography.titleLarge,
            color = QrTextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {},
            enabled = false,
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = "Filtrar historial",
                tint = QrTextSecondary,
            )
        }
        IconButton(onClick = onClearHistoryClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Borrar historial",
                tint = QrTextSecondary,
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilter.values().forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filterLabel(filter)) },
            )
        }
    }
}

private fun filterLabel(filter: HistoryFilter): String {
    return when (filter) {
        HistoryFilter.ALL -> "Todos"
        HistoryFilter.SAFE -> "Seguros"
        HistoryFilter.SUSPICIOUS -> "Sospechosos"
        HistoryFilter.DANGEROUS -> "Peligrosos"
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryUiItem,
    onClick: () -> Unit,
) {
    val riskColor = when (item.riskLevel) {
        RiskLevel.SAFE -> QrGreenDark
        RiskLevel.SUSPICIOUS, RiskLevel.UNKNOWN -> QrSuspicious
        RiskLevel.DANGEROUS -> QrDanger
    }
    val riskSoftColor = when (item.riskLevel) {
        RiskLevel.SAFE -> QrGreenSoft
        RiskLevel.SUSPICIOUS, RiskLevel.UNKNOWN -> QrSuspiciousSoft
        RiskLevel.DANGEROUS -> QrDangerSoft
    }
    val riskBadge = when (item.riskLevel) {
        RiskLevel.SAFE -> "SEGURA"
        RiskLevel.SUSPICIOUS, RiskLevel.UNKNOWN -> "SOSPECHOSA"
        RiskLevel.DANGEROUS -> "PELIGROSA"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(QrCardBackground)
            .semantics {
                role = Role.Button
                contentDescription = "Abrir detalle del análisis de ${item.title}"
            }
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(riskSoftColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (item.riskLevel == RiskLevel.DANGEROUS) Icons.Outlined.Info else Icons.Outlined.Security,
                        contentDescription = "Riesgo ${riskBadge.lowercase()}",
                        tint = riskColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = QrTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = QrTextSecondary,
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = QrTextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RiskBadge(
                    text = riskBadge,
                    background = riskSoftColor,
                    textColor = riskColor,
                )
                // analysisStatus se muestra como contexto de calidad del análisis
                // dentro de la card (no como filtro principal) para evitar
                // confundir señal de riesgo (riskLevel) con completitud técnica.
                if (item.analysisStatus == AnalysisStatus.PARTIAL) {
                    StatusBadge(text = "Análisis parcial")
                } else if (item.analysisStatus == AnalysisStatus.UNAVAILABLE) {
                    StatusBadge(text = "No completado")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = QrTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RiskBadge(
    text: String,
    background: Color,
    textColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(QrOutline)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = QrTextSecondary,
        )
    }
}

@Composable
private fun HistoryEmptyState(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(QrCardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = "Sin resultados en historial",
                tint = QrIconMuted,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = QrTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = QrTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryBottomBar(
    onScanTabClick: () -> Unit,
    onSettingsTabClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(QrBottomBarBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BottomBarItem(
            label = "ESCANEAR",
            icon = Icons.Outlined.QrCode2,
            selected = false,
            onClick = onScanTabClick,
        )
        BottomBarItem(
            label = "HISTORIAL",
            icon = Icons.Outlined.History,
            selected = true,
            onClick = {},
        )
        BottomBarItem(
            label = "AJUSTES",
            icon = Icons.Outlined.Settings,
            selected = false,
            onClick = onSettingsTabClick,
        )
    }
}

@Composable
private fun RowScope.BottomBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
