package com.followme.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(serverUrl = authRepository.currentServerUrl()) }
        }
    }

    fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun login() {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci l'indirizzo del server") }
            return
        }
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci email e password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.setServerUrl(state.serverUrl.trim())
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
