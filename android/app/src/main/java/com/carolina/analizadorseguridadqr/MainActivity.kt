package com.carolina.analizadorseguridadqr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.carolina.analizadorseguridadqr.ui.screen.MainScreen
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme
import com.carolina.analizadorseguridadqr.viewmodel.MainViewModel

// Activity minima: conecta ViewModel + Compose.
// Aqui no metemos logica de negocio, solo coordinacion de UI.
class MainActivity : ComponentActivity() {
    // El ViewModel vive asociado al ciclo de vida de esta Activity.
    private val viewModel: MainViewModel by viewModels()

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
                    onShowLoading = viewModel::onScanButtonClicked,
                    onShowIdle = viewModel::showIdle,
                    onShowMockReadyUrl = viewModel::showMockReadyUrl,
                    onShowMockResult = viewModel::showMockResult,
                )
            }
        }
    }
}
