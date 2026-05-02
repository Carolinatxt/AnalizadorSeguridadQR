package com.carolina.analizadorseguridadqr.ui.screen

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.security.OpenLinkPolicy
import com.carolina.analizadorseguridadqr.ui.security.resolveOpenLinkPolicy
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
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
    onOpenLink: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (uiState) {
        ScanUiState.Idle -> HomeStateScreen(
            onStartScan = onStartScan,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
        ScanUiState.Loading -> LoadingStateScreen(
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
        is ScanUiState.NotAWebUrl -> NotWebUrlStateScreen(
            message = uiState.message,
            onStartScan = onStartScan,
            onShowIdle = onShowIdle,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
        is ScanUiState.Error -> ErrorStateScreen(
            message = uiState.message,
            onStartScan = onStartScan,
            onShowIdle = onShowIdle,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
        // ReadyToAnalyze es transitorio; visualmente usamos la pantalla de carga.
        is ScanUiState.ReadyToAnalyze -> LoadingStateScreen(
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
        is ScanUiState.AnalysisResult -> AnalysisResultStateScreen(
            result = uiState,
            onShowIdle = onShowIdle,
            onOpenLink = onOpenLink,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun HomeStateScreen(
    onStartScan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = QrBackground,
        bottomBar = {
            VisualBottomBar(
                selected = BottomBarItem.Scan,
                onHistoryClick = onOpenHistory,
                onSettingsClick = onOpenSettings,
            )
        },
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
private fun LoadingStateScreen(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = QrBackground,
        bottomBar = {
            VisualBottomBar(
                selected = BottomBarItem.Scan,
                onHistoryClick = onOpenHistory,
                onSettingsClick = onOpenSettings,
            )
        },
    ) { paddingValues ->
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
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
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
        illustration = { NotWebIllustration() },
        title = "Este QR no contiene un enlace web",
        subtitle = subtitle,
        buttonsTopSpacerWeight = 1.35f,
        primaryText = "Escanear otro QR",
        primaryIcon = Icons.Outlined.QrCode2,
        onPrimaryClick = onStartScan,
        secondaryText = "Volver",
        onSecondaryClick = onShowIdle,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun ErrorStateScreen(
    message: String,
    onStartScan: () -> Unit,
    onShowIdle: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
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
        illustration = { AnalysisErrorIllustration() },
        title = "No se puede analizar el enlace ahora mismo",
        subtitle = subtitle,
        primaryText = "Escanear otro QR",
        primaryIcon = Icons.Outlined.QrCode2,
        onPrimaryClick = onStartScan,
        secondaryText = "Volver al inicio",
        onSecondaryClick = onShowIdle,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun AnalysisResultStateScreen(
    result: ScanUiState.AnalysisResult,
    onShowIdle: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AnalysisResultScreen(
        result = result,
        onShowIdle = onShowIdle,
        onOpenLink = onOpenLink,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun AnalysisResultScreen(
    result: ScanUiState.AnalysisResult,
    onShowIdle: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiModel = buildAnalysisResultUiModel(result)
    val domain = extractDisplayDomain(result.analyzedUrl)
    val openLinkPolicy = resolveOpenLinkPolicy(
        riskLevel = result.riskLevel,
        analysisStatus = result.analysisStatus,
    )
    var showOpenLinkConfirmation by rememberSaveable { mutableStateOf(false) }

    val requestOpenLink = {
        if (openLinkPolicy == OpenLinkPolicy.SafeDirectOpen) {
            onOpenLink(result.analyzedUrl.orEmpty())
        } else {
            showOpenLinkConfirmation = true
        }
    }

    Scaffold(
        containerColor = QrBackground,
        bottomBar = {
            VisualBottomBar(
                selected = BottomBarItem.Scan,
                onHistoryClick = onOpenHistory,
                onSettingsClick = onOpenSettings,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ResultTopBar(onBack = onShowIdle)
                Spacer(modifier = Modifier.height(24.dp))

                ResultHero(model = uiModel)
                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = uiModel.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = uiModel.accentColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiModel.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = QrTextSecondary,
                    textAlign = TextAlign.Center,
                )
                ReasonsCard(reasons = result.reasons)
                Spacer(modifier = Modifier.height(26.dp))

                DetectedDomainCard(
                    domain = domain,
                    accentColor = uiModel.accentColor,
                    accentSoftColor = uiModel.softColor,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            ResultActions(
                openLinkPolicy = openLinkPolicy,
                onShowIdle = onShowIdle,
                onOpenLink = requestOpenLink,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showOpenLinkConfirmation) {
        OpenLinkConfirmationDialog(
            openLinkPolicy = openLinkPolicy,
            analysisStatus = result.analysisStatus,
            onDismiss = { showOpenLinkConfirmation = false },
            onConfirm = {
                showOpenLinkConfirmation = false
                onOpenLink(result.analyzedUrl.orEmpty())
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReasonsCardPreview() {
    AnalizadorSeguridadQRTheme {
        Box(
            modifier = Modifier
                .background(QrBackground)
                .padding(24.dp),
        ) {
            ReasonsCard(
                reasons = listOf(
                    "Se han detectado indicios de robo de datos o suplantación.",
                    "Se han detectado señales compatibles con software malicioso.",
                    "El enlace aparece marcado como inseguro.",
                ),
            )
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
    illustration: @Composable () -> Unit,
    title: String,
    subtitle: String,
    buttonsTopSpacerWeight: Float = 1f,
    primaryText: String,
    primaryIcon: ImageVector?,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = QrBackground,
        bottomBar = {
            VisualBottomBar(
                selected = BottomBarItem.Scan,
                onHistoryClick = onOpenHistory,
                onSettingsClick = onOpenSettings,
            )
        },
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

            illustration()

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
            // Permite separar mas o menos el texto de los botones segun el estado.
            Spacer(modifier = Modifier.weight(buttonsTopSpacerWeight))

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
private fun NotWebIllustration() {
    // Ilustracion ligera: blobs suaves + tarjeta central blanca.
    Box(
        modifier = Modifier.size(144.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-18).dp, y = (-16).dp)
                .size(width = 96.dp, height = 104.dp)
                .clip(RoundedCornerShape(topStart = 52.dp, topEnd = 44.dp, bottomEnd = 56.dp, bottomStart = 40.dp))
                .background(QrOutline.copy(alpha = 0.78f)),
        )
        Box(
            modifier = Modifier
                .offset(x = 22.dp, y = 22.dp)
                .size(84.dp)
                .clip(CircleShape)
                .background(QrGreenSoft.copy(alpha = 0.9f)),
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(QrCardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = QrIconMuted.copy(alpha = 0.92f),
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun AnalysisErrorIllustration() {
    // Tarjeta limpia con nube "sin conexion" y punto rojo decorativo.
    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(QrCardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = QrIconMuted,
                modifier = Modifier.size(66.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = 14.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(QrDanger),
        )
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
            .semantics { role = Role.Button }
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
            .semantics { role = Role.Button }
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
private fun ResultTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = QrTextSecondary,
            )
        }
        Text(
            text = "Resultado",
            style = MaterialTheme.typography.titleLarge,
            color = QrGreenDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = "Historial",
                tint = QrTextSecondary,
            )
        }
    }
}

@Composable
private fun ResultHero(model: AnalysisResultUiModel) {
    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(model.softColor.copy(alpha = 0.45f)),
        )
        Box(
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
                .background(QrCardBackground)
                .border(
                    width = 4.dp,
                    color = model.accentColor.copy(alpha = 0.75f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = model.heroIcon,
                contentDescription = null,
                tint = model.accentColor,
                modifier = Modifier.size(58.dp),
            )
        }
    }
}

@Composable
private fun DetectedDomainCard(
    domain: String,
    accentColor: Color,
    accentSoftColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(QrCardBackground)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentSoftColor.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "DOMINIO DETECTADO",
                    style = MaterialTheme.typography.labelMedium,
                    color = QrTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = domain,
                    style = MaterialTheme.typography.titleMedium,
                    color = QrTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ReasonsCard(reasons: List<String>) {
    val visibleReasons = reasons
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(4)
    if (visibleReasons.isEmpty()) return

    Column {
        Spacer(modifier = Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(QrCardBackground)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column {
                Text(
                    text = "¿Por qué?",
                    style = MaterialTheme.typography.titleMedium,
                    color = QrTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleReasons.forEach { reason ->
                        ReasonRow(reason = reason)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasonRow(reason: String) {
    Row {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = QrTextSecondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = QrTextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResultActions(
    openLinkPolicy: OpenLinkPolicy,
    onShowIdle: () -> Unit,
    onOpenLink: () -> Unit,
) {
    when (openLinkPolicy) {
        OpenLinkPolicy.SafeDirectOpen -> {
            PrimaryActionButton(
                text = "Abrir enlace",
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = onOpenLink,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SecondaryActionButton(
                text = "Volver al inicio",
                onClick = onShowIdle,
            )
        }

        OpenLinkPolicy.ConfirmRiskyOpen,
        OpenLinkPolicy.ConfirmDangerousOpen -> {
            PrimaryActionButton(
                text = "Volver al inicio",
                icon = Icons.Outlined.Home,
                onClick = onShowIdle,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SecondaryActionButton(
                text = if (openLinkPolicy == OpenLinkPolicy.ConfirmDangerousOpen) {
                    "Entiendo el riesgo, abrir enlace"
                } else {
                    "Abrir de todas formas"
                },
                onClick = onOpenLink,
            )
        }
    }
}

@Composable
private fun OpenLinkConfirmationDialog(
    openLinkPolicy: OpenLinkPolicy,
    analysisStatus: AnalysisStatus,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isDangerous = openLinkPolicy == OpenLinkPolicy.ConfirmDangerousOpen
    val isAnalysisIncompleteOrUncertain = analysisStatus == AnalysisStatus.PARTIAL ||
        analysisStatus == AnalysisStatus.UNAVAILABLE ||
        analysisStatus == AnalysisStatus.UNKNOWN
    val title = if (isDangerous) "Enlace peligroso" else "Antes de continuar"
    val message = if (isDangerous) {
        "Este enlace ha sido relacionado con una amenaza grave. Abrirlo puede ponerte en riesgo. " +
            "Si continúas, se abrirá fuera de la app en el navegador del dispositivo."
    } else {
        // PARTIAL, UNAVAILABLE y UNKNOWN no deben parecer análisis plenamente fiables.
        if (isAnalysisIncompleteOrUncertain) {
            "Este enlace no se ha clasificado como seguro o el análisis no pudo completarse del todo. " +
                "Si continúas, se abrirá fuera de la app en el navegador del dispositivo."
        } else {
            "Este enlace no se ha clasificado como seguro. " +
                "Si continúas, se abrirá fuera de la app en el navegador del dispositivo."
        }
    }
    val confirmText = if (isDangerous) "Entiendo el riesgo" else "Abrir de todas formas"
    val confirmColor = if (isDangerous) QrDanger else QrTextSecondary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = QrTextPrimary) },
        text = { Text(text = message, color = QrTextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = QrTextSecondary)
            }
        },
    )
}

private data class AnalysisResultUiModel(
    val title: String,
    val description: String,
    val accentColor: Color,
    val softColor: Color,
    val heroIcon: ImageVector,
)

private fun buildAnalysisResultUiModel(result: ScanUiState.AnalysisResult): AnalysisResultUiModel {
    return when (result.riskLevel) {
        RiskLevel.DANGEROUS -> AnalysisResultUiModel(
            title = "Peligrosa",
            description = pickResultDescription(
                summary = result.summary,
                fallback = "Este enlace se ha clasificado como peligroso por señales graves detectadas durante el análisis.",
            ),
            accentColor = QrDanger,
            softColor = QrDangerSoft,
            heroIcon = Icons.Outlined.ErrorOutline,
        )
        RiskLevel.SAFE -> AnalysisResultUiModel(
            title = "Segura",
            description = pickResultDescription(
                summary = result.summary,
                fallback = "No se han detectado amenazas conocidas en este enlace.",
            ),
            accentColor = QrGreenDark,
            softColor = QrGreenSoft,
            heroIcon = Icons.Outlined.Security,
        )
        RiskLevel.SUSPICIOUS,
        RiskLevel.UNKNOWN -> AnalysisResultUiModel(
            title = "Sospechosa",
            description = pickResultDescription(
                summary = result.summary,
                fallback = "No hay información suficiente para clasificar este enlace como seguro.",
            ),
            accentColor = QrSuspicious,
            softColor = QrSuspiciousSoft,
            heroIcon = Icons.Outlined.Info,
        )
    }
}
private fun pickResultDescription(summary: String, fallback: String): String {
    val cleanSummary = summary.trim()
    if (cleanSummary.isBlank()) return fallback
    if (cleanSummary.length > 140) return fallback
    return cleanSummary
}

// Solo para mostrar el dominio al usuario en la UI.
// No usar esta función como validación de seguridad.
private fun extractDisplayDomain(url: String?): String {
    val cleanUrl = url?.trim().orEmpty()
    if (cleanUrl.isBlank()) return "dominio-no-disponible"

    return try {
        val host = Uri.parse(cleanUrl).host?.removePrefix("www.")?.trim().orEmpty()
        if (host.isNotBlank()) host else cleanUrl.take(48)
    } catch (exception: Exception) {
        Log.w("MainScreen", "extractDisplayDomain: fallo al parsear dominio (${exception.javaClass.simpleName})")
        cleanUrl.take(48)
    }
}

private enum class BottomBarItem {
    Scan,
    History,
    Settings,
}

// Barra inferior solo visual en esta fase; sin navegación funcional todavía.
@Composable
private fun VisualBottomBar(
    selected: BottomBarItem,
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
        BottomBarVisualItem(
            modifier = Modifier.weight(1f),
            label = "ESCANEAR",
            icon = Icons.Outlined.QrCode2,
            selected = selected == BottomBarItem.Scan,
            onClick = onScanClick,
        )
        BottomBarVisualItem(
            modifier = Modifier.weight(1f),
            label = "HISTORIAL",
            icon = Icons.Outlined.History,
            selected = selected == BottomBarItem.History,
            onClick = onHistoryClick,
        )
        BottomBarVisualItem(
            modifier = Modifier.weight(1f),
            label = "AJUSTES",
            icon = Icons.Outlined.Settings,
            selected = selected == BottomBarItem.Settings,
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun BottomBarVisualItem(
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
            onOpenLink = {},
            onOpenHistory = {},
            onOpenSettings = {},
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
            onOpenLink = {},
            onOpenHistory = {},
            onOpenSettings = {},
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
            onOpenLink = {},
            onOpenHistory = {},
            onOpenSettings = {},
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
            onOpenLink = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}
