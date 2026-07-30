package com.followme.app.data.remote

import com.followme.app.data.remote.dto.AccessTokenResponse
import com.followme.app.data.remote.dto.CredentialsRequest
import com.followme.app.data.remote.dto.OkResponse
import com.followme.app.data.remote.dto.RefreshRequest
import com.followme.app.data.remote.dto.TokenPairResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @Headers("$HEADER_SKIP_AUTH: true")
    @POST("api/auth/register")
    suspend fun register(@Body body: CredentialsRequest): Response<TokenPairResponse>

    @Headers("$HEADER_SKIP_AUTH: true")
    @POST("api/auth/login")
    suspend fun login(@Body body: CredentialsRequest): Response<TokenPairResponse>

    @Headers("$HEADER_SKIP_AUTH: true")
    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<AccessTokenResponse>

    @Headers("$HEADER_SKIP_AUTH: true")
    @POST("api/auth/logout-all")
    suspend fun logoutAll(@Body body: RefreshRequest): Response<OkResponse>

    companion object {
        /** Marker header stripped by [AuthInterceptor]; tells it to skip attaching an access token. */
        const val HEADER_SKIP_AUTH = "X-Skip-Auth"
    }
}
