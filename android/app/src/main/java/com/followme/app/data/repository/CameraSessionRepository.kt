package com.followme.app.data.repository

import com.followme.app.data.local.SessionCache
import com.followme.app.data.local.TokenStore
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.DeviceApi
import com.followme.app.data.remote.apiCall
import com.followme.app.data.remote.dto.PairDeviceRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DeviceSession(val deviceId: String, val deviceName: String)

/** Session/config for this app instance's camera ("videocamera remota") role - separate from [AuthRepository], which is the controller role's session. */
class CameraSessionRepository(
    private val deviceApi: DeviceApi,
    private val tokenStore: TokenStore,
    private val sessionCache: SessionCache,
) {
    val appRole: Flow<String?> = tokenStore.appRole

    val deviceSession: Flow<DeviceSession?> = combine(tokenStore.deviceId, tokenStore.deviceName) { id, name ->
        if (id != null && name != null) DeviceSession(id, name) else null
    }

    suspend fun currentServerUrl(): String = tokenStore.currentServerUrl()

    suspend fun setServerUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        tokenStore.saveServerUrl(normalized)
        sessionCache.serverUrl = normalized
    }

    suspend fun chooseRole(role: AppRole) {
        tokenStore.saveAppRole(role.value)
    }

    suspend fun pairDevice(pairingToken: String): ApiResult<DeviceSession> {
        val result = apiCall { deviceApi.pairDevice(PairDeviceRequest(pairingToken)) }
        if (result is ApiResult.Success) {
            sessionCache.deviceToken = result.data.deviceToken
            tokenStore.saveDeviceSession(result.data.deviceToken, result.data.deviceId, result.data.name)
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(DeviceSession(result.data.deviceId, result.data.name))
            is ApiResult.Failure -> result
        }
    }

    suspend fun unpairDevice() {
        sessionCache.deviceToken = null
        tokenStore.clearDeviceSession()
    }
}

enum class AppRole(val value: String) {
    CONTROLLER("controller"),
    CAMERA("camera"),
}
