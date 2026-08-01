package com.followme.app.ui.devicedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.data.repository.DeviceRepository
import com.followme.app.ui.GenericViewModelFactory
import com.followme.app.ui.theme.RecordingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceRepository: DeviceRepository,
    deviceId: String,
    deviceName: String,
    onViewRecordings: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: DeviceDetailViewModel = viewModel(
        factory = GenericViewModelFactory { DeviceDetailViewModel(deviceRepository, deviceId, deviceName) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.deviceName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Connessione: ${onlineLabel(uiState.online)}")
                    Text(
                        text = "Registrazione: ${uiState.recordingState?.let { recordingStateLabel(it) } ?: "sconosciuto"}",
                        color = if (uiState.recordingState == "error") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Avvia registrazione", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.startRecording("audio") },
                    enabled = !uiState.isSending,
                    modifier = Modifier.weight(1f),
                ) { Text("Audio") }
                OutlinedButton(
                    onClick = { viewModel.startRecording("video") },
                    enabled = !uiState.isSending,
                    modifier = Modifier.weight(1f),
                ) { Text("Video") }
                OutlinedButton(
                    onClick = { viewModel.startRecording("audio_video") },
                    enabled = !uiState.isSending,
                    modifier = Modifier.weight(1f),
                ) { Text("Entrambi") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::stopRecording,
                enabled = !uiState.isSending,
                colors = ButtonDefaults.buttonColors(containerColor = RecordingRed),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ferma registrazione")
            }

            uiState.message?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Card {
                    Text(
                        text = message,
                        color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onViewRecordings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Vedi registrazioni")
            }
        }
    }
}

private fun recordingStateLabel(state: String): String = when (state) {
    "recording_started" -> "in corso"
    "recording_stopped" -> "ferma"
    "error" -> "errore sul dispositivo"
    else -> state
}

private fun onlineLabel(online: Boolean?): String = when (online) {
    true -> "online"
    false -> "offline"
    null -> "sconosciuto"
}
