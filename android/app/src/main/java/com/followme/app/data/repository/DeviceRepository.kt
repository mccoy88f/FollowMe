package com.followme.app.data.repository

import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.DeviceApi
import com.followme.app.data.remote.apiCall
import com.followme.app.data.remote.dto.CommandResponse
import com.followme.app.data.remote.dto.CreateDeviceRequest
import com.followme.app.data.remote.dto.CreateDeviceResponse
import com.followme.app.data.remote.dto.DeviceDto
import com.followme.app.data.remote.dto.CommandRequest
import com.followme.app.data.remote.dto.RegeneratePairingResponse
import com.followme.app.data.socket.DeviceRecordingStatusEvent
import com.followme.app.data.socket.DeviceStatusEvent
import com.followme.app.data.socket.RealtimeClient
import kotlinx.coroutines.flow.SharedFlow

class DeviceRepository(
    private val deviceApi: DeviceApi,
    private val realtimeClient: RealtimeClient,
) {
    val deviceStatusEvents: SharedFlow<DeviceStatusEvent> = realtimeClient.deviceStatus
    val recordingStatusEvents: SharedFlow<DeviceRecordingStatusEvent> = realtimeClient.recordingStatus

    fun connectRealtime() = realtimeClient.connect()
    fun disconnectRealtime() = realtimeClient.disconnect()

    suspend fun listDevices(): ApiResult<List<DeviceDto>> =
        when (val result = apiCall { deviceApi.listDevices() }) {
            is ApiResult.Success -> ApiResult.Success(result.data.devices)
            is ApiResult.Failure -> result
        }

    suspend fun createDevice(name: String): ApiResult<CreateDeviceResponse> =
        apiCall { deviceApi.createDevice(CreateDeviceRequest(name)) }

    suspend fun regeneratePairingToken(deviceId: String): ApiResult<RegeneratePairingResponse> =
        apiCall { deviceApi.regeneratePairingToken(deviceId) }

    suspend fun deleteDevice(deviceId: String): ApiResult<Unit> =
        when (val result = apiCall { deviceApi.deleteDevice(deviceId) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }

    suspend fun sendCommand(deviceId: String, action: String, type: String?): ApiResult<CommandResponse> =
        apiCall { deviceApi.sendCommand(deviceId, CommandRequest(action, type)) }
}
