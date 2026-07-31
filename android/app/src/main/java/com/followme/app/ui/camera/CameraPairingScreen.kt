package com.followme.app.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.data.repository.CameraSessionRepository
import com.followme.app.ui.GenericViewModelFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun CameraPairingScreen(
    cameraSessionRepository: CameraSessionRepository,
    onPaired: () -> Unit,
) {
    val viewModel: CameraPairingViewModel = viewModel(
        factory = GenericViewModelFactory { CameraPairingViewModel(cameraSessionRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.paired) {
        if (uiState.paired) onPaired()
    }

    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        QrCodeScannerDialog(
            onCodeScanned = { rawCode ->
                showScanner = false
                val code = try {
                    val json = Json.parseToJsonElement(rawCode).jsonObject
                    json["code"]?.jsonPrimitive?.content ?: rawCode
                } catch (e: Exception) {
                    rawCode
                }
                viewModel.onPairingTokenChange(code.trim())
            },
            onDismiss = { showScanner = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Associa questo dispositivo", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Inquadra il QR Code dal dispositivo principale oppure inserisci qui il codice generato.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showScanner = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scansiona QR Code")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = viewModel::onServerUrlChange,
            label = { Text("Indirizzo server") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.pairingToken,
            onValueChange = viewModel::onPairingTokenChange,
            label = { Text("Codice di associazione") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = viewModel::pair,
            enabled = !uiState.isPairing,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (uiState.isPairing) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
            } else {
                Text("Associa")
            }
        }
    }
}
