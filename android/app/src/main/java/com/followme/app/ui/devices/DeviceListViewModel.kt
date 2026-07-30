package com.followme.app.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.dto.DeviceDto
import com.followme.app.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val devices: List<DeviceDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class DeviceListViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    init {
        loadDevices()
        deviceRepository.connectRealtime()
        viewModelScope.launch {
            deviceRepository.deviceStatusEvents.collect { event ->
                _uiState.update { state ->
                    state.copy(
                        devices = state.devices.map { device ->
                            if (device.id == event.deviceId) device.copy(online = event.online) else device
                        }
                    )
                }
            }
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = deviceRepository.listDevices()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, devices = result.data) }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        deviceRepository.disconnectRealtime()
    }
}
