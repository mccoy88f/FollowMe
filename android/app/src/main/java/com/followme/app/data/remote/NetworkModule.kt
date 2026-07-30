package com.followme.app.data.remote

import com.followme.app.BuildConfig
import com.followme.app.data.local.AuthEventBus
import com.followme.app.data.local.SessionCache
import com.followme.app.data.local.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Wires up OkHttp/Retrofit. The Retrofit base URL is a fixed, unused
 * placeholder - [DynamicBaseUrlInterceptor] rewrites every request to the
 * server URL the user configured, which can change at runtime without
 * rebuilding this whole module.
 */
class NetworkModule(
    sessionCache: SessionCache,
    tokenStore: TokenStore,
    authEventBus: AuthEventBus,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val dynamicBaseUrlInterceptor = DynamicBaseUrlInterceptor(sessionCache)

    /** Used only inside [TokenAuthenticator] to call the refresh endpoint without recursing into itself. */
    private val refreshOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val refreshRetrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE_URL)
        .client(refreshOkHttpClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private val tokenAuthenticator = TokenAuthenticator(
        sessionCache = sessionCache,
        tokenStore = tokenStore,
        authEventBus = authEventBus,
        refreshApi = { refreshRetrofit.create(AuthApi::class.java) },
    )

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(AuthHeaderInterceptor(sessionCache))
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val deviceApi: DeviceApi by lazy { retrofit.create(DeviceApi::class.java) }
    val recordingApi: RecordingApi by lazy { retrofit.create(RecordingApi::class.java) }
    val deviceRecordingApi: DeviceRecordingApi by lazy { retrofit.create(DeviceRecordingApi::class.java) }

    companion object {
        private val PLACEHOLDER_BASE_URL = "http://followme.invalid/".toHttpUrl()
    }
}
