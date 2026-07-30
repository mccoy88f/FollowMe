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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.data.repository.CameraSessionRepository
import com.followme.app.ui.GenericViewModelFactory

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
            text = "Nell'app di controllo, crea un nuovo dispositivo e inserisci qui il codice generato.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

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
