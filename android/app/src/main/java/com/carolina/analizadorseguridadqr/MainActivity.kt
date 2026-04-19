package com.carolina.analizadorseguridadqr

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.carolina.analizadorseguridadqr.ui.screen.MainScreen
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.viewmodel.MainViewModel

// Activity minima: conecta ViewModel + Compose.
// Aqui no metemos logica de negocio, solo coordinacion de UI.
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    // El ViewModel vive asociado al ciclo de vida de esta Activity.
    private val viewModel: MainViewModel by viewModels()
    private lateinit var scanner: GmsBarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupScanner()

        setContent {
            AnalizadorSeguridadQRTheme {
                // Convertimos StateFlow en estado observable por Compose.
                val uiState by viewModel.uiState.collectAsState()

                // La pantalla solo recibe estado actual + acciones de usuario.
                MainScreen(
                    uiState = uiState,
                    onStartScan = ::startScan,
                    onShowIdle = viewModel::showIdle,
                )
            }
        }
    }

    private fun setupScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        scanner = GmsBarcodeScanning.getClient(this, options)
    }

    // La Activity solo lanza escaner y delega el resultado al ViewModel.
    private fun startScan() {
        viewModel.onScanButtonClicked()

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                viewModel.onScanResult(barcode.rawValue)
            }
            .addOnCanceledListener {
                viewModel.showIdle()
            }
            .addOnFailureListener { error ->
                Log.d(TAG, "Fallo al escanear QR: ${error.message}")
                viewModel.showError("No se pudo completar el escaneo. Intentalo de nuevo.")
            }
    }
}
