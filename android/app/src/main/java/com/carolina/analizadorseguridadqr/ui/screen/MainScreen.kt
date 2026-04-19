package com.carolina.analizadorseguridadqr.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carolina.analizadorseguridadqr.ui.state.ScanUiState
import com.carolina.analizadorseguridadqr.ui.theme.AnalizadorSeguridadQRTheme

// Pantalla principal de prueba para la V0.
// Objetivo: visualizar de forma clara el estado actual.
@Composable
fun MainScreen(
    uiState: ScanUiState,
    onShowLoading: () -> Unit,
    onShowIdle: () -> Unit,
    onShowMockReadyUrl: () -> Unit,
    onShowMockResult: () -> Unit,
) {
    // Layout simple en columna para mantener el codigo facil de seguir.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Analizador Seguridad QR",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "Base V0: prueba de estados con ViewModel + StateFlow.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = onShowLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Boton temporal para simular que se inicia un proceso.
            Text("Probar Loading")
        }

        Button(
            onClick = onShowIdle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Volver a Idle")
        }

        Button(
            onClick = onShowMockReadyUrl,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Mostrar URL mock")
        }

        Button(
            onClick = onShowMockResult,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Mostrar resultado mock")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estado actual",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // El contenido cambia segun el estado emitido por el ViewModel.
                StateContent(uiState = uiState)
            }
        }
    }
}

@Composable
private fun StateContent(uiState: ScanUiState) {
    // Renderizado declarativo: cada estado pinta un bloque distinto.
    when (uiState) {
        ScanUiState.Idle -> {
            Text("Idle: esperando una accion del usuario.")
        }

        ScanUiState.Loading -> {
            Text("Loading: simulando proceso de escaneo.")
        }

        is ScanUiState.NotAWebUrl -> {
            Text("URL no web: ${uiState.message}")
        }

        is ScanUiState.ReadyToAnalyze -> {
            Text("Lista para analizar: ${uiState.url}")
        }

        is ScanUiState.AnalysisResult -> {
            Text("risk_level: ${uiState.riskLevel}")
            Text("analysis_status: ${uiState.analysisStatus}")
            Text("summary: ${uiState.summary}")
        }

        is ScanUiState.Error -> {
            Text("Error: ${uiState.message}")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    AnalizadorSeguridadQRTheme {
        MainScreen(
            uiState = ScanUiState.Idle,
            onShowLoading = {},
            onShowIdle = {},
            onShowMockReadyUrl = {},
            onShowMockResult = {},
        )
    }
}
