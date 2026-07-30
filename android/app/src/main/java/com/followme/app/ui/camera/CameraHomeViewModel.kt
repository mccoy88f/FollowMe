package com.followme.app.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.followme.app.data.repository.CameraSessionRepository
import com.followme.app.data.repository.DeviceSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraHomeViewModel(private val cameraSessionRepository: CameraSessionRepository) : ViewModel() {
    val deviceSession: StateFlow<DeviceSession?> = cameraSessionRepository.deviceSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun unpair(onDone: () -> Unit) {
        viewModelScope.launch {
            cameraSessionRepository.unpairDevice()
            onDone()
        }
    }
}
