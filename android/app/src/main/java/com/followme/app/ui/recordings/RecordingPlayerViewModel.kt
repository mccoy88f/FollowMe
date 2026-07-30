package com.followme.app.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

data class RecordingPlayerUiState(
    val isDownloading: Boolean = false,
    val downloadMessage: String? = null,
)

class RecordingPlayerViewModel(
    private val recordingRepository: RecordingRepository,
    private val recordingId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordingPlayerUiState())
    val uiState: StateFlow<RecordingPlayerUiState> = _uiState.asStateFlow()

    val streamingUrl: String = recordingRepository.streamingUrl(recordingId)

    fun downloadTo(sink: OutputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadMessage = null) }
            val message = when (val result = recordingRepository.downloadTo(recordingId, sink)) {
                is ApiResult.Success -> "Download completato"
                is ApiResult.Failure -> result.message
            }
            _uiState.update { it.copy(isDownloading = false, downloadMessage = message) }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(downloadMessage = null) }
}
