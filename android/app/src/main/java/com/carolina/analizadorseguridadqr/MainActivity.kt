package com.carolina.analizadorseguridadqr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.carolina.analizadorseguridadqr.ui.screen.MainScreen
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.viewmodel.MainViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// Activity minima: conecta ViewModel + Compose.
// Aqui no metemos logica de negocio, solo coordinacion de UI.
class MainActivity : ComponentActivity() {
    // El ViewModel vive asociado al ciclo de vida de esta Activity.
    private val viewModel: MainViewModel by viewModels()

    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents?.trim()
        if (content.isNullOrEmpty()) {
            viewModel.showIdle()
            return@registerForActivityResult
        }

        viewModel.onScanResult(content)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
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
            AnalizadorSeguridadQRTheme {
                // Convertimos StateFlow en estado observable por Compose.
                val uiState by viewModel.uiState.collectAsState()

                // La pantalla solo recibe estado actual + acciones de usuario.
                MainScreen(
                    uiState = uiState,
                    onStartScan = ::startScan,
                    onShowIdle = viewModel::showIdle,
                    onRetryAnalysis = viewModel::retryLastAnalysis,
                )
            }
        }
    }

    // La Activity solo lanza escaner y delega el resultado al ViewModel.
    private fun startScan() {
        viewModel.onScanButtonClicked()

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
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
}
