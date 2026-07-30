package com.followme.app.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.data.repository.DeviceRepository
import com.followme.app.ui.GenericViewModelFactory
import com.followme.app.ui.common.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    deviceRepository: DeviceRepository,
    onDone: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: AddDeviceViewModel = viewModel(
        factory = GenericViewModelFactory { AddDeviceViewModel(deviceRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nuovo dispositivo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val created = uiState.created
            if (created == null) {
                Text(
                    text = "Dai un nome al dispositivo che userai come videocamera remota (es. \"Salotto\", \"Ufficio\").",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nome dispositivo") },
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
                    onClick = viewModel::createDevice,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    Text("Crea e genera codice")
                }
                TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Annulla")
                }
            } else {
                Text(text = "Dispositivo \"${created.deviceName}\" creato", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apri FollowMe sul telefono da usare come videocamera e inserisci questo codice quando richiesto.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(
                            text = created.pairingToken,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scade il ${formatTimestamp(created.expiresAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                OutlinedButton(
                    onClick = viewModel::regeneratePairingToken,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    Text("Genera nuovo codice")
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("Fatto")
                }
            }
        }
    }
}
