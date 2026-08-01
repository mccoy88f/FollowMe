package com.followme.app.data.remote

import com.followme.app.data.local.SessionCache
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches a Bearer token to every request, except those explicitly marked
 * with [AuthApi.HEADER_SKIP_AUTH] (login, register, refresh, logout-all,
 * device pairing - endpoints that either need no auth or authenticate via
 * a token in the body instead).
 *
 * Requests to `api/device/...` (the camera role's own recording-upload API)
 * use [SessionCache.deviceToken] instead of the controller role's
 * [SessionCache.accessToken] - both can be populated in the same process
 * since a single app build supports either role.
 */
class AuthHeaderInterceptor(private val sessionCache: SessionCache) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header(AuthApi.HEADER_SKIP_AUTH) != null) {
            val stripped = original.newBuilder().removeHeader(AuthApi.HEADER_SKIP_AUTH).build()
            return chain.proceed(stripped)
        }

        val isDeviceEndpoint = original.url.encodedPath.startsWith("/api/device/")
        val token = if (isDeviceEndpoint) sessionCache.deviceToken else sessionCache.accessToken

        val authorized = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(authorized)
    }
}
