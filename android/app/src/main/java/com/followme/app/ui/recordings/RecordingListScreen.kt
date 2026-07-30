package com.followme.app.ui.recordings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.followme.app.data.remote.dto.RecordingDto
import com.followme.app.data.repository.RecordingRepository
import com.followme.app.ui.GenericViewModelFactory
import com.followme.app.ui.common.formatBytes
import com.followme.app.ui.common.formatDuration
import com.followme.app.ui.common.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingListScreen(
    recordingRepository: RecordingRepository,
    deviceId: String,
    deviceName: String,
    onOpenRecording: (recordingId: String, type: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: RecordingListViewModel = viewModel(
        factory = GenericViewModelFactory { RecordingListViewModel(recordingRepository, deviceId, deviceName) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrazioni - ${uiState.deviceName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading && uiState.recordings.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.recordings.isEmpty() -> {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                uiState.recordings.isEmpty() -> {
                    Text(
                        text = "Nessuna registrazione ancora.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(uiState.recordings, key = { it.id }) { recording ->
                            RecordingRow(
                                recording = recording,
                                onClick = { onOpenRecording(recording.id, recording.type) },
                                onDelete = { viewModel.delete(recording.id) },
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
private fun RecordingRow(recording: RecordingDto, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (recording.type == "audio") Icons.Filled.Mic else Icons.Filled.Videocam,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.padding(start = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formatTimestamp(recording.startedAt), style = MaterialTheme.typography.titleMedium)
                val statusLabel = when (recording.status) {
                    "recording" -> "In corso"
                    "failed" -> "Fallita"
                    else -> formatDuration(recording.durationSeconds)
                }
                Text(
                    text = "$statusLabel · ${formatBytes(recording.bytesReceived.toLongOrNull() ?: 0L)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina")
            }
        }
    }
}
