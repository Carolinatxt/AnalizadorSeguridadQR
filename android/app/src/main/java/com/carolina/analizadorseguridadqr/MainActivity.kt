package com.carolina.analizadorseguridadqr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carolina.analizadorseguridadqr.data.local.history.AppDatabase
import com.carolina.analizadorseguridadqr.data.preferences.ThemePreferencesRepository
import com.carolina.analizadorseguridadqr.data.repository.ScanHistoryRepository
import com.carolina.analizadorseguridadqr.security.isSupportedWebUrl
import com.carolina.analizadorseguridadqr.ui.history.HistoryDetailDialog
import com.carolina.analizadorseguridadqr.ui.history.HistoryFilter
import com.carolina.analizadorseguridadqr.ui.history.HistoryOpenLinkConfirmationDialog
import com.carolina.analizadorseguridadqr.ui.history.HistoryScreen
import com.carolina.analizadorseguridadqr.ui.history.HistoryUiItem
import com.carolina.analizadorseguridadqr.ui.history.HistoryViewModel
import com.carolina.analizadorseguridadqr.ui.history.HistoryViewModelFactory
import com.carolina.analizadorseguridadqr.ui.navigation.AppTab
import com.carolina.analizadorseguridadqr.ui.screen.MainScreen
import com.carolina.analizadorseguridadqr.ui.settings.AppearanceSettingsScreen
import com.carolina.analizadorseguridadqr.ui.settings.HistorySettingsScreen
import com.carolina.analizadorseguridadqr.ui.settings.PrivacySettingsScreen
import com.carolina.analizadorseguridadqr.ui.security.resolveOpenLinkPolicy
import com.carolina.analizadorseguridadqr.ui.settings.SettingsHomeScreen
import com.carolina.analizadorseguridadqr.ui.settings.SettingsSubScreen
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.ui.theme.ThemeMode
import com.carolina.analizadorseguridadqr.ui.theme.resolveDarkTheme
import com.carolina.analizadorseguridadqr.viewmodel.MainViewModel
import com.carolina.analizadorseguridadqr.viewmodel.MainViewModelFactory
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

// Activity mínima: conecta ViewModel + Compose.
// Aquí no metemos lógica de negocio, solo coordinación de UI.
class MainActivity : ComponentActivity() {
    private val historyRepository: ScanHistoryRepository by lazy {
        val database = AppDatabase.getInstance(applicationContext)
        ScanHistoryRepository(database.scanHistoryDao())
    }

    private val themePreferencesRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepository(applicationContext)
    }

    // El ViewModel vive asociado al ciclo de vida de esta Activity.
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(historyRepository)
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(historyRepository)
    }

    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents?.trim()
        if (content.isNullOrEmpty()) {
            viewModel.showIdle()
            return@registerForActivityResult
        }

        viewModel.onScanResult(content)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            launchScanner()
        } else {
            viewModel.showError("Para escanear códigos QR necesitas permitir el acceso a la cámara.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by themePreferencesRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM,
            )
            val themeScope = rememberCoroutineScope()

            AnalizadorSeguridadQRTheme(
                darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme()),
            ) {
                // Convertimos StateFlow en estado observable por Compose.
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val historyItems by historyViewModel.historyItems.collectAsStateWithLifecycle()
                var selectedTab by rememberSaveable { mutableStateOf(AppTab.SCAN) }
                var settingsSubScreen by rememberSaveable {
                    mutableStateOf(SettingsSubScreen.HOME)
                }
                var selectedHistoryFilter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
                var selectedHistoryItem by remember { mutableStateOf<HistoryUiItem?>(null) }
                var pendingHistoryOpenItem by remember { mutableStateOf<HistoryUiItem?>(null) }
                var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
                var showHistoryOpenLinkConfirmation by rememberSaveable { mutableStateOf(false) }

                fun switchTab(tab: AppTab) {
                    if (selectedTab == AppTab.SCAN && tab != AppTab.SCAN) {
                        // Al salir de SCAN cancelamos cualquier analisis activo para que
                        // no reaparezca un resultado antiguo cuando el usuario vuelva.
                        viewModel.showIdle()
                    }

                    selectedTab = tab
                    settingsSubScreen = SettingsSubScreen.HOME
                    showClearHistoryDialog = false
                    showHistoryOpenLinkConfirmation = false
                    pendingHistoryOpenItem = null
                    selectedHistoryItem = null
                }

                BackHandler(
                    enabled = selectedTab != AppTab.SCAN ||
                        settingsSubScreen != SettingsSubScreen.HOME,
                ) {
                    if (
                        selectedTab == AppTab.SETTINGS &&
                        settingsSubScreen != SettingsSubScreen.HOME
                    ) {
                        settingsSubScreen = SettingsSubScreen.HOME
                    } else {
                        switchTab(AppTab.SCAN)
                    }
                }

                when (selectedTab) {
                    AppTab.SCAN -> MainScreen(
                        uiState = uiState,
                        onStartScan = ::startScan,
                        onShowIdle = viewModel::showIdle,
                        onRetryAnalysis = viewModel::retryLastAnalysis,
                        onManualUrlSubmitted = viewModel::onManualUrlSubmitted,
                        onManualUrlChanged = viewModel::onManualUrlChanged,
                        onOpenLink = ::openUrlInExternalBrowser,
                        onOpenHistory = { switchTab(AppTab.HISTORY) },
                        onOpenSettings = { switchTab(AppTab.SETTINGS) },
                    )

                    AppTab.HISTORY -> HistoryScreen(
                        historyItems = historyItems,
                        selectedFilter = selectedHistoryFilter,
                        onFilterSelected = { selectedHistoryFilter = it },
                        onBackClick = { switchTab(AppTab.SCAN) },
                        onClearHistoryClick = { showClearHistoryDialog = true },
                        onItemClick = { selectedHistoryItem = it },
                        onScanTabClick = { switchTab(AppTab.SCAN) },
                        onSettingsTabClick = { switchTab(AppTab.SETTINGS) },
                    )

                    AppTab.SETTINGS -> when (settingsSubScreen) {
                        SettingsSubScreen.HOME -> SettingsHomeScreen(
                            currentThemeMode = themeMode,
                            onBackClick = { switchTab(AppTab.SCAN) },
                            onAppearanceClick = {
                                settingsSubScreen = SettingsSubScreen.APPEARANCE
                            },
                            onHistoryShortcutClick = {
                                settingsSubScreen = SettingsSubScreen.HOME
                                switchTab(AppTab.HISTORY)
                            },
                            onHistorySettingsClick = {
                                settingsSubScreen = SettingsSubScreen.HISTORY
                            },
                            onPrivacyClick = {
                                settingsSubScreen = SettingsSubScreen.PRIVACY
                            },
                            onScanTabClick = { switchTab(AppTab.SCAN) },
                            onHistoryTabClick = { switchTab(AppTab.HISTORY) },
                            onSettingsTabClick = {},
                        )

                        SettingsSubScreen.APPEARANCE -> AppearanceSettingsScreen(
                            selectedThemeMode = themeMode,
                            onThemeModeSelected = { selectedMode ->
                                if (selectedMode != themeMode) {
                                    themeScope.launch {
                                        themePreferencesRepository.setThemeMode(selectedMode)
                                    }
                                }
                            },
                            onBackClick = { settingsSubScreen = SettingsSubScreen.HOME },
                            onScanTabClick = { switchTab(AppTab.SCAN) },
                            onHistoryTabClick = { switchTab(AppTab.HISTORY) },
                            onSettingsTabClick = {},
                        )

                        SettingsSubScreen.HISTORY -> HistorySettingsScreen(
                            hasHistory = historyItems.isNotEmpty(),
                            onBackClick = { settingsSubScreen = SettingsSubScreen.HOME },
                            onClearHistoryClick = { showClearHistoryDialog = true },
                            onScanTabClick = { switchTab(AppTab.SCAN) },
                            onHistoryTabClick = { switchTab(AppTab.HISTORY) },
                            onSettingsTabClick = {},
                        )

                        SettingsSubScreen.PRIVACY -> PrivacySettingsScreen(
                            onBackClick = { settingsSubScreen = SettingsSubScreen.HOME },
                            onScanTabClick = { switchTab(AppTab.SCAN) },
                            onHistoryTabClick = { switchTab(AppTab.HISTORY) },
                            onSettingsTabClick = {},
                        )
                    }
                }

                if (showClearHistoryDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearHistoryDialog = false },
                        title = { Text("Borrar historial") },
                        text = {
                            Text("Esta acción eliminará los análisis guardados en este dispositivo.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showClearHistoryDialog = false
                                    historyViewModel.clearHistory()
                                },
                            ) {
                                Text("Borrar historial")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearHistoryDialog = false }) {
                                Text("Cancelar")
                            }
                        },
                    )
                }

                selectedHistoryItem?.let { historyItem ->
                    HistoryDetailDialog(
                        item = historyItem,
                        onDismiss = {
                            pendingHistoryOpenItem = null
                            selectedHistoryItem = null
                        },
                        onRequestOpenLink = {
                            // Historial: confirmamos siempre la apertura, incluso si el
                            // análisis fue SAFE+COMPLETE, porque es un snapshot temporal.
                            pendingHistoryOpenItem = historyItem
                            selectedHistoryItem = null
                            showHistoryOpenLinkConfirmation = true
                        },
                        onReanalyzeLink = {
                            val urlToReanalyze = historyItem.url

                            selectedHistoryItem = null
                            pendingHistoryOpenItem = null
                            showHistoryOpenLinkConfirmation = false
                            showClearHistoryDialog = false

                            if (!urlToReanalyze.isNullOrBlank()) {
                                switchTab(AppTab.SCAN)
                                viewModel.analyzeUrlFromHistory(urlToReanalyze)
                            }
                        },
                    )
                }

                if (showHistoryOpenLinkConfirmation) {
                    val currentItem = pendingHistoryOpenItem
                    if (currentItem == null) {
                        showHistoryOpenLinkConfirmation = false
                    } else {
                        val openLinkPolicy = resolveOpenLinkPolicy(
                            riskLevel = currentItem.riskLevel,
                            analysisStatus = currentItem.analysisStatus,
                        )
                        HistoryOpenLinkConfirmationDialog(
                            openLinkPolicy = openLinkPolicy,
                            riskLevel = currentItem.riskLevel,
                            analysisStatus = currentItem.analysisStatus,
                            onDismiss = {
                                pendingHistoryOpenItem = null
                                showHistoryOpenLinkConfirmation = false
                            },
                            onConfirm = {
                                showHistoryOpenLinkConfirmation = false
                                val urlToOpen = currentItem.url
                                pendingHistoryOpenItem = null
                                selectedHistoryItem = null
                                openUrlInExternalBrowser(urlToOpen)
                            },
                        )
                    }
                }
            }
        }
    }

    // La Activity solo lanza el escáner y delega el resultado al ViewModel.
    private fun startScan() {
        viewModel.onScanButtonClicked()

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        launchScanner()
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Enfoca el código QR")
            setBeepEnabled(false)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }

        qrScannerLauncher.launch(options)
    }

    private fun openUrlInExternalBrowser(url: String) {
        val cleanUrl = url.trim()
        val invalidUrlMessage = "No se puede abrir este enlace porque no parece una URL web válida."
        if (cleanUrl.isBlank()) {
            Toast.makeText(
                this,
                invalidUrlMessage,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        if (!isSupportedWebUrl(cleanUrl)) {
            Toast.makeText(
                this,
                invalidUrlMessage,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val parsedUri = Uri.parse(cleanUrl)

        val externalOpenIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        try {
            startActivity(externalOpenIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No se encontró una aplicación compatible para abrir el enlace.",
                Toast.LENGTH_SHORT,
            ).show()
        } catch (_: SecurityException) {
            Toast.makeText(
                this,
                "No se pudo abrir el enlace de forma segura.",
                Toast.LENGTH_SHORT,
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "No se pudo abrir el enlace.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
