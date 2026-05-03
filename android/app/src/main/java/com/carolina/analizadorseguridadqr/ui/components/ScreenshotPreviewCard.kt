package com.carolina.analizadorseguridadqr.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.carolina.analizadorseguridadqr.network.ScreenshotService
import com.carolina.analizadorseguridadqr.ui.security.ScreenshotPreviewPolicy
import com.carolina.analizadorseguridadqr.ui.security.resolveScreenshotPreviewPolicy
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
import com.carolina.analizadorseguridadqr.ui.theme.QrCardBackground
import com.carolina.analizadorseguridadqr.ui.theme.QrDanger
import com.carolina.analizadorseguridadqr.ui.theme.QrDangerSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrGreenDark
import com.carolina.analizadorseguridadqr.ui.theme.QrOutline
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspicious
import com.carolina.analizadorseguridadqr.ui.theme.QrSuspiciousSoft
import com.carolina.analizadorseguridadqr.ui.theme.QrTextPrimary
import com.carolina.analizadorseguridadqr.ui.theme.QrTextSecondary
import kotlinx.coroutines.launch

sealed class ScreenshotUiState {
    data object Idle : ScreenshotUiState()
    data object Loading : ScreenshotUiState()
    data class Loaded(val imageUrl: String) : ScreenshotUiState()
    data class Error(val message: String) : ScreenshotUiState()
}

private const val SCREENSHOT_BLOCKED_MESSAGE =
    "No se genera vista previa de este enlace por seguridad."
private const val SCREENSHOT_UNAVAILABLE_MESSAGE =
    "La vista previa no esta disponible en este momento."
private const val SCREENSHOT_RENDER_ERROR_MESSAGE =
    "No se pudo mostrar la vista previa."

@Composable
fun ScreenshotPreviewCard(
    analyzedUrl: String,
    riskLevel: RiskLevel,
    analysisStatus: AnalysisStatus,
    screenshotService: ScreenshotService? = null,
    modifier: Modifier = Modifier,
) {
    if (analyzedUrl.isBlank()) return

    val scope = rememberCoroutineScope()
    val imageLoader = rememberSecureScreenshotImageLoader()
    val effectiveScreenshotService = screenshotService ?: remember { ScreenshotService() }

    var screenshotUiState by remember(analyzedUrl) {
        mutableStateOf<ScreenshotUiState>(ScreenshotUiState.Idle)
    }
    var showConfirmationDialog by remember(analyzedUrl) { mutableStateOf(false) }
    var showFullScreenPreview by remember(analyzedUrl) { mutableStateOf(false) }

    fun requestScreenshotFromBackend() {
        if (screenshotUiState == ScreenshotUiState.Loading) {
            return
        }

        screenshotUiState = ScreenshotUiState.Loading
        scope.launch {
            try {
                val response = effectiveScreenshotService.createScreenshot(analyzedUrl)
                screenshotUiState = when {
                    response.available && !response.imageUrl.isNullOrBlank() -> {
                        ScreenshotUiState.Loaded(response.imageUrl)
                    }

                    response.available -> {
                        ScreenshotUiState.Error("La vista previa no esta disponible.")
                    }

                    else -> {
                        ScreenshotUiState.Error(
                            response.message.takeIf { it.isNotBlank() }
                                ?: SCREENSHOT_UNAVAILABLE_MESSAGE,
                        )
                    }
                }
            } catch (_: Exception) {
                screenshotUiState = ScreenshotUiState.Error(SCREENSHOT_UNAVAILABLE_MESSAGE)
            }
        }
    }

    fun handleScreenshotRequest(hasUserConfirmed: Boolean = false) {
        when (resolveScreenshotPreviewPolicy(riskLevel, analysisStatus)) {
            ScreenshotPreviewPolicy.AllowDirect -> requestScreenshotFromBackend()
            ScreenshotPreviewPolicy.RequireConfirmation -> {
                if (hasUserConfirmed) {
                    requestScreenshotFromBackend()
                } else {
                    showConfirmationDialog = true
                }
            }

            ScreenshotPreviewPolicy.Blocked -> {
                showConfirmationDialog = false
                screenshotUiState = ScreenshotUiState.Error(SCREENSHOT_BLOCKED_MESSAGE)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(QrCardBackground)
            .padding(18.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(QrSuspiciousSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = QrSuspicious,
                    )
                }
                Column {
                    Text(
                        text = "Vista previa avanzada",
                        style = MaterialTheme.typography.titleMedium,
                        color = QrTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Opcional y bajo demanda. No afecta al resultado de seguridad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = QrTextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (val state = screenshotUiState) {
                ScreenshotUiState.Idle -> {
                    Text(
                        text = "Si lo necesitas, puedes generar una captura visual de la pagina analizada. Una pagina fraudulenta puede parecer legitima.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QrTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ScreenshotActionButton(
                        text = "Generar vista previa",
                        onClick = ::handleScreenshotRequest,
                    )
                }

                ScreenshotUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = QrGreenDark,
                            trackColor = QrOutline,
                            strokeWidth = 3.dp,
                        )
                        Column {
                            Text(
                                text = "Generando vista previa...",
                                style = MaterialTheme.typography.titleSmall,
                                color = QrTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Esto puede tardar unos segundos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = QrTextSecondary,
                            )
                        }
                    }
                }

                is ScreenshotUiState.Loaded -> {
                    ScreenshotPreviewImage(
                        imageUrl = state.imageUrl,
                        imageLoader = imageLoader,
                        contentDescription = "Vista previa de la pagina analizada",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showFullScreenPreview = true },
                        onError = {
                            screenshotUiState = ScreenshotUiState.Error(SCREENSHOT_RENDER_ERROR_MESSAGE)
                        },
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    ScreenshotWarningBlock(
                        backgroundColor = QrDangerSoft,
                        accentColor = QrDanger,
                        title = "Vista previa orientativa. Una pagina fraudulenta puede parecer legitima. Esta imagen no garantiza que el sitio sea seguro.",
                        message = "La vista previa puede no reflejar el estado actual de la pagina y puede no estar disponible en algunos sitios.",
                    )
                }

                is ScreenshotUiState.Error -> {
                    ScreenshotWarningBlock(
                        backgroundColor = QrSuspiciousSoft,
                        accentColor = QrSuspicious,
                        title = state.message,
                        message = "La vista previa no forma parte de la decision principal de seguridad.",
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ScreenshotActionButton(
                        text = "Intentar de nuevo",
                        onClick = ::handleScreenshotRequest,
                    )
                }
            }
        }
    }

    if (showConfirmationDialog) {
        ScreenshotConfirmationDialog(
            onDismiss = { showConfirmationDialog = false },
            onConfirm = {
                showConfirmationDialog = false
                handleScreenshotRequest(hasUserConfirmed = true)
            },
        )
    }

    val loadedState = screenshotUiState as? ScreenshotUiState.Loaded
    if (showFullScreenPreview && loadedState != null) {
        ScreenshotFullScreenDialog(
            imageUrl = loadedState.imageUrl,
            imageLoader = imageLoader,
            onDismiss = { showFullScreenPreview = false },
            onError = {
                showFullScreenPreview = false
                screenshotUiState = ScreenshotUiState.Error(SCREENSHOT_RENDER_ERROR_MESSAGE)
            },
        )
    }
}

@Composable
private fun ScreenshotPreviewImage(
    imageUrl: String,
    imageLoader: ImageLoader,
    contentDescription: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
) {
    SubcomposeAsyncImage(
        model = imageUrl,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            ScreenshotImageLoadingState(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        },
        success = {
            SubcomposeAsyncImageContent()
        },
        error = {
            onError()
        },
    )
}

@Composable
private fun ScreenshotImageLoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(QrOutline),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = QrGreenDark,
                trackColor = Color.White,
                strokeWidth = 3.dp,
            )
            Text(
                text = "Cargando vista previa...",
                style = MaterialTheme.typography.bodyMedium,
                color = QrTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ScreenshotFullScreenDialog(
    imageUrl: String,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onError: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ScreenshotPreviewImage(
                imageUrl = imageUrl,
                imageLoader = imageLoader,
                contentDescription = "Vista previa ampliada de la pagina analizada",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                onError = onError,
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar vista previa",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ScreenshotActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = QrGreenDark,
            contentColor = Color.White,
            disabledContainerColor = QrOutline,
            disabledContentColor = QrTextSecondary,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ScreenshotWarningBlock(
    backgroundColor: Color,
    accentColor: Color,
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = QrTextSecondary,
            )
        }
    }
}

@Composable
private fun ScreenshotConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Antes de generar la vista previa",
                color = QrTextPrimary,
            )
        },
        text = {
            Text(
                text = "Esta URL tiene senales de riesgo o no ha podido analizarse completamente.\n\nTen en cuenta que una pagina fraudulenta puede parecer legitima visualmente. La vista previa no garantiza que el sitio sea seguro.\n\nPara generar la vista previa, la URL se enviara a SnapRender, un proveedor externo de capturas web.\n\nQuieres continuar?",
                color = QrTextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Continuar", color = QrGreenDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = QrTextSecondary)
            }
        },
        containerColor = QrCardBackground,
        tonalElevation = 0.dp,
    )
}
