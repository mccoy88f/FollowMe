package com.followme.app

import android.content.Context
import com.followme.app.data.local.AuthEventBus
import com.followme.app.data.local.SessionCache
import com.followme.app.data.local.TokenStore
import com.followme.app.data.remote.NetworkModule
import com.followme.app.data.repository.AuthRepository
import com.followme.app.data.repository.CameraSessionRepository
import com.followme.app.data.repository.DeviceRecordingRepository
import com.followme.app.data.repository.DeviceRepository
import com.followme.app.data.repository.RecordingRepository
import com.followme.app.data.socket.DeviceSocketClient
import com.followme.app.data.socket.RealtimeClient
import kotlinx.coroutines.runBlocking

/** Simple hand-rolled DI container (no Hilt): builds every dependency once and hands out shared instances. Created in [FollowMeApp.onCreate]. */
class AppContainer(context: Context) {
    val tokenStore = TokenStore(context.applicationContext)

    // Read once, synchronously, at process start so OkHttp interceptors have
    // an initial value to work with; kept in sync thereafter by the
    // repositories (see AuthRepository/CameraSessionRepository).
    val sessionCache: SessionCache = runBlocking {
        SessionCache(
            initialServerUrl = tokenStore.currentServerUrl(),
            initialDeviceToken = tokenStore.currentDeviceToken(),
        ).apply {
            accessToken = tokenStore.currentAccessToken()
            refreshToken = tokenStore.currentRefreshToken()
        }
    }

    private val authEventBus = AuthEventBus()
    private val networkModule = NetworkModule(sessionCache, tokenStore, authEventBus)
    private val realtimeClient = RealtimeClient(sessionCache)

    val authRepository = AuthRepository(networkModule.authApi, tokenStore, sessionCache, authEventBus)
    val deviceRepository = DeviceRepository(networkModule.deviceApi, realtimeClient)
    val recordingRepository = RecordingRepository(networkModule.recordingApi, sessionCache)
    val cameraSessionRepository = CameraSessionRepository(networkModule.deviceApi, tokenStore, sessionCache)
    val deviceRecordingRepository = DeviceRecordingRepository(networkModule.deviceRecordingApi)
    val deviceSocketClient = DeviceSocketClient(sessionCache)

    /** Shared authenticated OkHttpClient, exposed for ExoPlayer's data source (auth header + dynamic server URL must apply to media playback requests too). */
    val okHttpClient = networkModule.okHttpClient
}
