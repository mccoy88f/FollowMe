package com.followme.app.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val serverUrl: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registerSuccess: Boolean = false,
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(serverUrl = authRepository.currentServerUrl()) }
        }
    }

    fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }

    fun register() {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci l'indirizzo del server") }
            return
        }
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci email e password") }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(error = "La password deve avere almeno 8 caratteri") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "Le password non coincidono") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.setServerUrl(state.serverUrl.trim())
            when (val result = authRepository.register(state.email.trim(), state.password)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
