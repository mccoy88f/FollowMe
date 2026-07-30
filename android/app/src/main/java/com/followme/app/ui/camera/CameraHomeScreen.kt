package com.followme.app.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.camera.CameraForegroundService
import com.followme.app.data.repository.CameraSessionRepository
import com.followme.app.ui.GenericViewModelFactory
import com.followme.app.ui.theme.OfflineGray
import com.followme.app.ui.theme.OnlineGreen
import com.followme.app.ui.theme.RecordingRed

private val requiredPermissions: Array<String>
    get() = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

@Composable
fun CameraHomeScreen(
    cameraSessionRepository: CameraSessionRepository,
    onUnpaired: () -> Unit,
) {
    val viewModel: CameraHomeViewModel = viewModel(
        factory = GenericViewModelFactory { CameraHomeViewModel(cameraSessionRepository) }
    )
    val deviceSession by viewModel.deviceSession.collectAsState()
    val serviceState by CameraForegroundService.state.collectAsState()
    val context = LocalContext.current

    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            CameraForegroundService.start(context)
        } else {
            permissionDenied = true
        }
    }

    val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
    val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true

    Scaffold(
        topBar = { TopAppBar(title = { Text("Videocamera remota") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(text = deviceSession?.deviceName ?: "Dispositivo", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow(
                        label = "Server",
                        active = serviceState.connected,
                        activeText = "Connesso",
                        inactiveText = "Non connesso",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusRow(
                        label = "Registrazione",
                        active = serviceState.recording,
                        activeText = "In corso",
                        inactiveText = "Ferma",
                        activeColor = RecordingRed,
                    )
                    serviceState.lastError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Modalità videocamera attiva", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = serviceState.running,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val missing = requiredPermissions.any {
                                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (missing) {
                                permissionLauncher.launch(requiredPermissions)
                            } else {
                                CameraForegroundService.start(context)
                            }
                        } else {
                            CameraForegroundService.stop(context)
                        }
                    },
                )
            }

            if (permissionDenied) {
                Text(
                    text = "Servono i permessi di fotocamera e microfono per attivare la modalità videocamera.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isIgnoringBatteryOptimizations) {
                Text(
                    text = "Per evitare che il sistema interrompa la registrazione in background, disattiva il risparmio energetico per FollowMe.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disattiva risparmio energetico")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            OutlinedButton(
                onClick = {
                    CameraForegroundService.stop(context)
                    viewModel.unpair(onUnpaired)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Rimuovi associazione dispositivo")
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    active: Boolean,
    activeText: String,
    inactiveText: String,
    activeColor: androidx.compose.ui.graphics.Color = OnlineGreen,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = if (active) activeColor else OfflineGray, shape = CircleShape)
        )
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(text = "$label: ${if (active) activeText else inactiveText}", style = MaterialTheme.typography.bodyMedium)
    }
}
