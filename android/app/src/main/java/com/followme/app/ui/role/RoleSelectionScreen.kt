package com.followme.app.ui.role

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.followme.app.data.repository.AppRole
import com.followme.app.data.repository.CameraSessionRepository
import kotlinx.coroutines.launch

@Composable
fun RoleSelectionScreen(
    cameraSessionRepository: CameraSessionRepository,
    onRoleChosen: (AppRole) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Benvenuto in FollowMe", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Come vuoi usare questo telefono?",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    cameraSessionRepository.chooseRole(AppRole.CONTROLLER)
                    onRoleChosen(AppRole.CONTROLLER)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Controllo remoto (comando le videocamere)")
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    cameraSessionRepository.chooseRole(AppRole.CAMERA)
                    onRoleChosen(AppRole.CAMERA)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Videocamera remota (questo telefono registra)")
        }
    }
}
