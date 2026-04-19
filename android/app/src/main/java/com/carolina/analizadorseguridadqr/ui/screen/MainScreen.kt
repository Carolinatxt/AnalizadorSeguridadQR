package com.carolina.analizadorseguridadqr.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.state.ScanUiState
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.ui.theme.QrBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrBottomBarSelected
import com.carolina.analizadorseguridadqr.ui.theme.QrCardBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrDanger
import com.carolina.analizadorseguridadqr.ui.theme.QrDangerSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenDark
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenLight
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrIconMuted
import com.carolina.analizadorseguridadqr.ui.theme.QrOutline
import com.carolina.analizadorseguridadqr.ui.theme.QrSecondaryButton
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspicious
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspiciousSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary

// Pantalla raíz: Compose representa estado y dispara callbacks.
@Composable
fun MainScreen(
    uiState: ScanUiState,
    onStartScan: () -> Unit,
    onShowIdle: () -> Unit,
    onRetryAnalysis: () -> Unit,
) {
    when (uiState) {
        ScanUiState.Idle -> HomeStateScreen(onStartScan = onStartScan)
        ScanUiState.Loading -> LoadingStateScreen()
        is ScanUiState.NotAWebUrl -> NotWebUrlStateScreen(
            message = uiState.message,
            onStartScan = onStartScan,
            onShowIdle = onShowIdle,
        )
        is ScanUiState.Error -> ErrorStateScreen(
            message = uiState.message,
            onRetry = onRetryAnalysis,
            onShowIdle = onShowIdle,
        )
        // ReadyToAnalyze es transitorio; visualmente usamos la pantalla de carga.
        is ScanUiState.ReadyToAnalyze -> LoadingStateScreen()
        is ScanUiState.AnalysisResult -> AnalysisResultStateScreen(
            result = uiState,
            onStartScan = onStartScan,
            onShowIdle = onShowIdle,
        )
    }
}

@Composable
private fun HomeStateScreen(onStartScan: () -> Unit) {
    Scaffold(
        containerColor = QrBackground,
        bottomBar = { VisualBottomBar(selected = BottomBarItem.Scan) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrTopBar(actionIcon = Icons.Outlined.History)
            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(QrCardBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        tint = QrIconMuted,
                        modifier = Modifier.size(58.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = 16.dp, y = 16.dp)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(QrGreenSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = QrGreenDark,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "¿Es seguro este QR?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = QrTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Protege tu identidad escaneando códigos QR de forma segura y privada.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(34.dp))

            PrimaryActionButton(
                text = "Escanear QR",
                icon = Icons.Outlined.QrCode2,
                onClick = onStartScan,
            )

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "COMPRUEBA UN QR",
                style = MaterialTheme.typography.labelLarge,
                color = QrTextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LoadingStateScreen() {
    Scaffold(containerColor = QrBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrTopBar(actionIcon = Icons.Outlined.History)
            Spacer(modifier = Modifier.weight(1f))

            CircularProgressIndicator(
                modifier = Modifier.size(70.dp),
                color = QrGreenDark,
                trackColor = QrOutline,
                strokeWidth = 4.dp,
            )
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = "Comprobando enlace...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = QrTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Estamos verificando la seguridad de tu destino digital para tu tranquilidad.",
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LoadingDot(isActive = true)
                LoadingDot(isActive = false)
                LoadingDot(isActive = false)
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun NotWebUrlStateScreen(
    message: String,
    onStartScan: () -> Unit,
    onShowIdle: () -> Unit,
) {
    val subtitle = if (message.isBlank()) {
        "La app solo puede analizar códigos QR que abren páginas web para garantizar tu seguridad digital."
    } else {
        message
    }

    InfoStateLayout(
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onShowIdle,
        actionIcon = Icons.Outlined.History,
        onActionClick = null,
        centerIcon = Icons.Outlined.Info,
        centerIconTint = QrTextSecondary,
        title = "Este QR no contiene un enlace web",
        subtitle = subtitle,
        primaryText = "Escanear otro QR",
        primaryIcon = Icons.Outlined.QrCode2,
        onPrimaryClick = onStartScan,
        secondaryText = "Volver",
        onSecondaryClick = onShowIdle,
    )
}

@Composable
private fun ErrorStateScreen(
    message: String,
    onRetry: () -> Unit,
    onShowIdle: () -> Unit,
) {
    val subtitle = if (message.isBlank() || message.length > 130) {
        "Comprueba tu conexión e inténtalo de nuevo."
    } else {
        message
    }

    InfoStateLayout(
        navigationIcon = null,
        onNavigationClick = null,
        actionIcon = Icons.Outlined.Close,
        onActionClick = onShowIdle,
        centerIcon = Icons.Outlined.ErrorOutline,
        centerIconTint = QrTextSecondary,
        title = "No se puede analizar el enlace ahora mismo",
        subtitle = subtitle,
        primaryText = "Reintentar",
        primaryIcon = null,
        onPrimaryClick = onRetry,
        secondaryText = "Volver al inicio",
        onSecondaryClick = onShowIdle,
    )
}

@Composable
private fun AnalysisResultStateScreen(
    result: ScanUiState.AnalysisResult,
    onStartScan: () -> Unit,
    onShowIdle: () -> Unit,
) {
    val visual = buildRiskVisual(result.riskLevel, result.analysisStatus)

    Scaffold(
        containerColor = QrBackground,
        bottomBar = { VisualBottomBar(selected = BottomBarItem.Scan) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrTopBar(actionIcon = Icons.Outlined.History)
            Spacer(modifier = Modifier.height(28.dp))

            ResultSummaryCard(visual = visual)
            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(QrCardBackground)
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Nivel de riesgo: ${result.riskLevel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = QrTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Estado del análisis: ${result.analysisStatus}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = QrTextSecondary,
                    )
                    Text(
                        text = "Resumen: ${result.summary}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = QrTextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            PrimaryActionButton(
                text = "Escanear otro QR",
                icon = Icons.Outlined.QrCode2,
                onClick = onStartScan,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SecondaryActionButton(
                text = "Volver al inicio",
                onClick = onShowIdle,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Base simple para pantallas informativas (no web y error).
@Composable
private fun InfoStateLayout(
    navigationIcon: ImageVector?,
    onNavigationClick: (() -> Unit)?,
    actionIcon: ImageVector?,
    onActionClick: (() -> Unit)?,
    centerIcon: ImageVector,
    centerIconTint: Color,
    title: String,
    subtitle: String,
    primaryText: String,
    primaryIcon: ImageVector?,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
) {
    Scaffold(
        containerColor = QrBackground,
        bottomBar = { VisualBottomBar(selected = BottomBarItem.Scan) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrTopBar(
                navigationIcon = navigationIcon,
                onNavigationClick = onNavigationClick,
                actionIcon = actionIcon,
                onActionClick = onActionClick,
            )
            Spacer(modifier = Modifier.height(46.dp))

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(QrCardBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = centerIcon,
                    contentDescription = null,
                    tint = centerIconTint,
                    modifier = Modifier.size(62.dp),
                )
            }

            Spacer(modifier = Modifier.height(42.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = QrTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))

            PrimaryActionButton(
                text = primaryText,
                icon = primaryIcon,
                onClick = onPrimaryClick,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SecondaryActionButton(
                text = secondaryText,
                onClick = onSecondaryClick,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QrTopBar(
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null && onNavigationClick != null) {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = "Volver",
                    tint = QrTextSecondary,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = QrGreenDark,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "QR Scanner",
                style = MaterialTheme.typography.titleLarge,
                color = QrGreenDark,
                fontWeight = FontWeight.Bold,
            )
        }

        if (actionIcon != null) {
            if (onActionClick != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = QrTextSecondary,
                    )
                }
            } else {
                Box(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = QrTextSecondary,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Brush.horizontalGradient(listOf(QrGreenDark, QrGreenLight)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(QrSecondaryButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = QrTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingDot(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isActive) 6.dp else 5.dp)
            .clip(CircleShape)
            .background(if (isActive) QrGreenDark else QrOutline),
    )
}

@Composable
private fun ResultSummaryCard(visual: ResultVisualModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(visual.backgroundColor)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(visual.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.color,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = visual.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = QrTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = visual.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QrTextSecondary,
                )
            }
        }
    }
}

private data class ResultVisualModel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color,
)

private fun buildRiskVisual(riskLevel: String, analysisStatus: String): ResultVisualModel {
    val normalized = "${riskLevel.trim()} ${analysisStatus.trim()}".lowercase()

    return when {
        normalized.contains("danger") ||
            normalized.contains("malicious") ||
            normalized.contains("phishing") ||
            normalized.contains("high") -> ResultVisualModel(
            title = "Riesgo alto detectado",
            subtitle = "Evita abrir este enlace y compruébalo manualmente.",
            icon = Icons.Outlined.ErrorOutline,
            color = QrDanger,
            backgroundColor = QrDangerSoft,
        )

        normalized.contains("suspicious") ||
            normalized.contains("medium") ||
            normalized.contains("unknown") -> ResultVisualModel(
            title = "Riesgo medio o dudoso",
            subtitle = "Revísalo con cuidado antes de continuar.",
            icon = Icons.Outlined.Info,
            color = QrSuspicious,
            backgroundColor = QrSuspiciousSoft,
        )

        else -> ResultVisualModel(
            title = "Sin señales críticas",
            subtitle = "No se detectó un riesgo alto en este análisis.",
            icon = Icons.Outlined.Security,
            color = QrGreenDark,
            backgroundColor = QrGreenSoft,
        )
    }
}

private enum class BottomBarItem {
    Scan,
    History,
    Settings,
}

// Barra inferior solo visual en esta fase; sin navegación funcional todavía.
@Composable
private fun VisualBottomBar(selected: BottomBarItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(QrBottomBarBackground)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BottomBarVisualItem(
            modifier = Modifier.weight(1f),
            label = "ESCANEAR",
            icon = Icons.Outlined.QrCode2,
            selected = selected == BottomBarItem.Scan,
        )
        BottomBarVisualItem(
            modifier = Modifier.weight(1f),
            label = "HISTORIAL",
            icon = Icons.Outlined.History,
            selected = selected == BottomBarItem.History,
        )
        BottomBarVisualItem(
            modifier = Modifier.weight(1f),
            label = "AJUSTES",
            icon = Icons.Outlined.Settings,
            selected = selected == BottomBarItem.Settings,
        )
    }
}

@Composable
private fun BottomBarVisualItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
) {
    val background = if (selected) QrBottomBarSelected else Color.Transparent
    val tint = if (selected) QrGreenDark else QrTextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AnalizadorSeguridadQRTheme {
        MainScreen(
            uiState = ScanUiState.Idle,
            onStartScan = {},
            onShowIdle = {},
            onRetryAnalysis = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() {
    AnalizadorSeguridadQRTheme {
        MainScreen(
            uiState = ScanUiState.Loading,
            onStartScan = {},
            onShowIdle = {},
            onRetryAnalysis = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotWebPreview() {
    AnalizadorSeguridadQRTheme {
        MainScreen(
            uiState = ScanUiState.NotAWebUrl("La app solo analiza enlaces web."),
            onStartScan = {},
            onShowIdle = {},
            onRetryAnalysis = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() {
    AnalizadorSeguridadQRTheme {
        MainScreen(
            uiState = ScanUiState.Error("Comprueba tu conexión e inténtalo de nuevo."),
            onStartScan = {},
            onShowIdle = {},
            onRetryAnalysis = {},
        )
    }
}
