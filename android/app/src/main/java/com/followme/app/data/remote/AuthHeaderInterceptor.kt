package com.followme.app.data.remote

import com.followme.app.data.local.SessionCache
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches the current access token as a Bearer header to every request,
 * except those explicitly marked with [AuthApi.HEADER_SKIP_AUTH] (login,
 * register, refresh, logout-all - endpoints that either need no auth or
 * authenticate via a refresh token in the body instead). */
class AuthHeaderInterceptor(private val sessionCache: SessionCache) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header(AuthApi.HEADER_SKIP_AUTH) != null) {
            val stripped = original.newBuilder().removeHeader(AuthApi.HEADER_SKIP_AUTH).build()
            return chain.proceed(stripped)
        }

        val accessToken = sessionCache.accessToken
        val authorized = if (accessToken != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
        } else {
            original
        }
        return chain.proceed(authorized)
    }
}
