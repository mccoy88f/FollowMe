package com.followme.app.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatedDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val pairingToken: String,
    val expiresAt: String,
)

data class AddDeviceUiState(
    val name: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val created: CreatedDeviceInfo? = null,
)

class AddDeviceViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }

    fun createDevice() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Inserisci un nome per il dispositivo") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            when (val result = deviceRepository.createDevice(name)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        created = CreatedDeviceInfo(
                            deviceId = result.data.device.id,
                            deviceName = result.data.device.name,
                            pairingToken = result.data.pairingToken,
                            expiresAt = result.data.pairingTokenExpiresAt,
                        ),
                    )
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isBusy = false, error = result.message) }
            }
        }
    }

    fun regeneratePairingToken() {
        val created = _uiState.value.created ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            when (val result = deviceRepository.regeneratePairingToken(created.deviceId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        created = created.copy(
                            pairingToken = result.data.pairingToken,
                            expiresAt = result.data.pairingTokenExpiresAt,
                        ),
                    )
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isBusy = false, error = result.message) }
            }
        }
    }
}
