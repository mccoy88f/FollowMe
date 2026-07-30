package com.followme.app.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.followme.app.data.repository.AuthRepository
import com.followme.app.ui.GenericViewModelFactory

@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: RegisterViewModel = viewModel(
        factory = GenericViewModelFactory { RegisterViewModel(authRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Crea un account", style = MaterialTheme.typography.titleLarge)
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
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password (min. 8 caratteri)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text("Conferma password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = viewModel::register,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Registrati")
            }
        }

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text("Hai già un account? Accedi")
        }
    }
}
