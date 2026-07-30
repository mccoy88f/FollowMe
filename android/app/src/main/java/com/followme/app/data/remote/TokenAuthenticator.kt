package com.followme.app.data.remote

import com.followme.app.data.local.AuthEventBus
import com.followme.app.data.local.SessionCache
import com.followme.app.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Response
import okhttp3.Route

/**
 * Runs on a 401 response: swaps in a fresh access token via the refresh
 * endpoint and retries the original request once. If the refresh token
 * itself is invalid/expired, clears the session and notifies [authEventBus]
 * so the UI can send the user back to the login screen.
 *
 * Uses [refreshApi], a Retrofit client built without this authenticator
 * (see NetworkModule), to avoid recursing into itself.
 */
class TokenAuthenticator(
    private val sessionCache: SessionCache,
    private val tokenStore: TokenStore,
    private val authEventBus: AuthEventBus,
    private val refreshApi: () -> AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): okhttp3.Request? {
        if (response.request.header(AuthApi.HEADER_SKIP_AUTH) != null) return null
        if (responseCount(response) >= 2) return null

        val refreshToken = sessionCache.refreshToken ?: run {
            authEventBus.notifyLoggedOut()
            return null
        }

        val newAccessToken = runBlocking {
            try {
                val result = refreshApi().refresh(
                    com.followme.app.data.remote.dto.RefreshRequest(refreshToken)
                )
                if (result.isSuccessful) result.body()?.accessToken else null
            } catch (e: Exception) {
                null
            }
        }

        if (newAccessToken == null) {
            sessionCache.clearTokens()
            runBlocking { tokenStore.clearTokens() }
            authEventBus.notifyLoggedOut()
            return null
        }

        sessionCache.accessToken = newAccessToken
        runBlocking { tokenStore.saveAccessToken(newAccessToken) }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
