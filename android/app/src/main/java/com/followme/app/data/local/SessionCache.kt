package com.followme.app.data.local

import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory mirror of the current session (kept in sync with [TokenStore]),
 * so OkHttp interceptors/authenticators - which run synchronously on a
 * background thread and must not block on DataStore's suspend API - can
 * read the current access token, refresh token and server URL instantly.
 *
 * [TokenStore] remains the source of truth across process restarts; this
 * cache is populated from it once at startup and updated in lock-step
 * whenever a repository persists a change.
 */
class SessionCache(initialServerUrl: String) {
    private val accessTokenRef = AtomicReference<String?>(null)
    private val refreshTokenRef = AtomicReference<String?>(null)
    private val serverUrlRef = AtomicReference(initialServerUrl)

    var accessToken: String?
        get() = accessTokenRef.get()
        set(value) = accessTokenRef.set(value)

    var refreshToken: String?
        get() = refreshTokenRef.get()
        set(value) = refreshTokenRef.set(value)

    var serverUrl: String
        get() = serverUrlRef.get()
        set(value) = serverUrlRef.set(value)

    fun setTokens(accessToken: String?, refreshToken: String?) {
        accessTokenRef.set(accessToken)
        refreshTokenRef.set(refreshToken)
    }

    fun clearTokens() {
        accessTokenRef.set(null)
        refreshTokenRef.set(null)
    }
}
