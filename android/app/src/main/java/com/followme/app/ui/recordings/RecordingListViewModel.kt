package com.followme.app.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.dto.RecordingDto
import com.followme.app.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordingListUiState(
    val deviceName: String,
    val recordings: List<RecordingDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class RecordingListViewModel(
    private val recordingRepository: RecordingRepository,
    private val deviceId: String,
    deviceName: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordingListUiState(deviceName = deviceName))
    val uiState: StateFlow<RecordingListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = recordingRepository.listRecordings(deviceId)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, recordings = result.data) }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun delete(recordingId: String) {
        viewModelScope.launch {
            when (val result = recordingRepository.deleteRecording(recordingId)) {
                is ApiResult.Success -> _uiState.update { state ->
                    state.copy(recordings = state.recordings.filterNot { it.id == recordingId })
                }
                is ApiResult.Failure -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }
}
