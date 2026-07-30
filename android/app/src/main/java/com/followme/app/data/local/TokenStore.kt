package com.followme.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.followme.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "followme_session")

/** Persists auth tokens and the configured backend URL across app restarts. */
class TokenStore(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val SERVER_URL = stringPreferencesKey("server_url")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[Keys.REFRESH_TOKEN] }
    val serverUrl: Flow<String> =
        context.dataStore.data.map { it[Keys.SERVER_URL] ?: BuildConfig.DEFAULT_SERVER_URL }

    suspend fun currentAccessToken(): String? = accessToken.first()
    suspend fun currentRefreshToken(): String? = refreshToken.first()
    suspend fun currentServerUrl(): String = serverUrl.first()

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[Keys.ACCESS_TOKEN] = accessToken
            it[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveAccessToken(accessToken: String) {
        context.dataStore.edit { it[Keys.ACCESS_TOKEN] = accessToken }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url }
    }

    suspend fun clearTokens() {
        context.dataStore.edit {
            it.remove(Keys.ACCESS_TOKEN)
            it.remove(Keys.REFRESH_TOKEN)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
