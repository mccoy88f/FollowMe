package com.followme.app.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.data.remote.dto.DeviceDto
import com.followme.app.data.repository.DeviceRepository
import com.followme.app.ui.GenericViewModelFactory
import com.followme.app.ui.common.formatTimestamp
import com.followme.app.ui.theme.OfflineGray
import com.followme.app.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    deviceRepository: DeviceRepository,
    onDeviceClick: (deviceId: String, deviceName: String) -> Unit,
    onAddDevice: () -> Unit,
    onLogout: () -> Unit,
) {
    val viewModel: DeviceListViewModel = viewModel(
        factory = GenericViewModelFactory { DeviceListViewModel(deviceRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei dispositivi") },
                actions = {
                    IconButton(onClick = viewModel::loadDevices) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Esci")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDevice) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi dispositivo")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading && uiState.devices.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.devices.isEmpty() -> {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                uiState.devices.isEmpty() -> {
                    Text(
                        text = "Nessun dispositivo ancora. Tocca + per aggiungerne uno.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(uiState.devices, key = { it.id }) { device ->
                            DeviceRow(
                                device = device,
                                recordingState = uiState.activeRecordings[device.id],
                                onClick = { onDeviceClick(device.id, device.name) },
                            )
                            Spacer(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceDto, recordingState: String?, onClick: () -> Unit) {
    val isRecording = recordingState == "recording_started"
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isRecording) com.followme.app.ui.theme.RecordingRed else if (device.online) OnlineGreen else OfflineGray,
                        shape = CircleShape,
                    )
            )
            Spacer(modifier = Modifier.padding(start = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                    if (isRecording) {
                        Text(
                            text = "🔴 IN REGISTRAZIONE",
                            style = MaterialTheme.typography.labelMedium,
                            color = com.followme.app.ui.theme.RecordingRed
                        )
                    }
                }
                Text(
                    text = if (isRecording) "Registrazione in corso sul dispositivo" else if (device.online) "Online" else "Ultima connessione: ${formatTimestamp(device.lastSeenAt, "mai")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) com.followme.app.ui.theme.RecordingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
