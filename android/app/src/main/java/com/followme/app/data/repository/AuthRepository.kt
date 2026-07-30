package com.followme.app.data.repository

import com.followme.app.data.local.AuthEventBus
import com.followme.app.data.local.SessionCache
import com.followme.app.data.local.TokenStore
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.AuthApi
import com.followme.app.data.remote.apiCall
import com.followme.app.data.remote.dto.CredentialsRequest
import com.followme.app.data.remote.dto.RefreshRequest
import com.followme.app.data.remote.dto.TokenPairResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val sessionCache: SessionCache,
    private val authEventBus: AuthEventBus,
) {
    val isLoggedIn: Flow<Boolean> = tokenStore.accessToken.map { it != null }
    val loggedOutEvents: SharedFlow<Unit> = authEventBus.loggedOut

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

    suspend fun register(email: String, password: String): ApiResult<Unit> =
        applyTokens(apiCall { authApi.register(CredentialsRequest(email, password)) })

    suspend fun login(email: String, password: String): ApiResult<Unit> =
        applyTokens(apiCall { authApi.login(CredentialsRequest(email, password)) })

    suspend fun logout() {
        val refreshToken = tokenStore.currentRefreshToken()
        sessionCache.clearTokens()
        tokenStore.clearTokens()
        if (refreshToken != null) {
            // Best-effort: revoke server-side too, but don't block logout on it.
            runCatching { authApi.logoutAll(RefreshRequest(refreshToken)) }
        }
    }

    private suspend fun applyTokens(result: ApiResult<TokenPairResponse>): ApiResult<Unit> {
        if (result is ApiResult.Success) {
            sessionCache.setTokens(result.data.accessToken, result.data.refreshToken)
            tokenStore.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }
}
