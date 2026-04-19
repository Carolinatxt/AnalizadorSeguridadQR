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
    onStartScan: () -> Unit,
    onShowIdle: () -> Unit,
) {
    // Layout simple en columna para mantener el código fácil de seguir.
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
            text = "Escanea un QR: se valida la URL y se consulta el backend automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Escanear QR")
        }

        Button(
            onClick = onShowIdle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Limpiar estado")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estado actual",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // El contenido cambia según el estado emitido por el ViewModel.
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
            Text("En espera: esperando una acción del usuario.")
        }

        ScanUiState.Loading -> {
            Text("Procesando QR y analizando URL...")
        }

        is ScanUiState.NotAWebUrl -> {
            Text("No se puede analizar este QR.")
            Text(uiState.message)
        }

        is ScanUiState.ReadyToAnalyze -> {
            Text("URL detectada y lista para analizar:")
            Text(uiState.url)
        }

        is ScanUiState.AnalysisResult -> {
            Text("Nivel de riesgo: ${uiState.riskLevel}")
            Text("Estado del análisis: ${uiState.analysisStatus}")
            Text("Resumen: ${uiState.summary}")
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
            onStartScan = {},
            onShowIdle = {},
        )
    }
}
