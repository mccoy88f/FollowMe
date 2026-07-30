package com.followme.app.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.repository.CameraSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraPairingUiState(
    val serverUrl: String = "",
    val pairingToken: String = "",
    val isPairing: Boolean = false,
    val error: String? = null,
    val paired: Boolean = false,
)

class CameraPairingViewModel(private val cameraSessionRepository: CameraSessionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraPairingUiState())
    val uiState: StateFlow<CameraPairingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(serverUrl = cameraSessionRepository.currentServerUrl()) }
        }
    }

    fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value) }
    fun onPairingTokenChange(value: String) = _uiState.update { it.copy(pairingToken = value, error = null) }

    fun pair() {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci l'indirizzo del server") }
            return
        }
        if (state.pairingToken.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci il codice mostrato nell'app di controllo") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, error = null) }
            cameraSessionRepository.setServerUrl(state.serverUrl.trim())
            when (val result = cameraSessionRepository.pairDevice(state.pairingToken.trim())) {
                is ApiResult.Success -> _uiState.update { it.copy(isPairing = false, paired = true) }
                is ApiResult.Failure -> _uiState.update { it.copy(isPairing = false, error = result.message) }
            }
        }
    }
}
