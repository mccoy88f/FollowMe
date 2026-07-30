package com.followme.app.ui.devicedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceDetailUiState(
    val deviceName: String,
    val isSending: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val recordingState: String? = null,
)

class DeviceDetailViewModel(
    private val deviceRepository: DeviceRepository,
    private val deviceId: String,
    deviceName: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceDetailUiState(deviceName = deviceName))
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deviceRepository.recordingStatusEvents.collect { event ->
                if (event.deviceId == deviceId) {
                    _uiState.update { it.copy(recordingState = event.state) }
                }
            }
        }
    }

    fun startRecording(type: String) = sendCommand(action = "start", type = type)
    fun stopRecording() = sendCommand(action = "stop", type = null)

    private fun sendCommand(action: String, type: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, message = null) }
            when (val result = deviceRepository.sendCommand(deviceId, action, type)) {
                is ApiResult.Success -> {
                    val delivered = result.data.delivered
                    val message = when {
                        !delivered -> "Dispositivo non raggiungibile (offline)"
                        action == "start" -> "Comando di avvio inviato"
                        else -> "Comando di stop inviato"
                    }
                    _uiState.update { it.copy(isSending = false, message = message, isError = !delivered) }
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(isSending = false, message = result.message, isError = true)
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
