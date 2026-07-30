package com.followme.app.data.remote

import com.followme.app.data.local.SessionCache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Rewrites every request's scheme/host/port to the currently configured
 * server URL ([SessionCache.serverUrl]), which the user can change at
 * runtime from the login screen. Retrofit is built once with a fixed
 * placeholder base URL; this interceptor is what actually makes it dynamic.
 */
class DynamicBaseUrlInterceptor(private val sessionCache: SessionCache) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // Thrown as IOException (not IllegalArgumentException) on a malformed
        // configured URL so it surfaces as a normal, catchable network
        // failure instead of crashing OkHttp's dispatcher thread.
        val target = try {
            sessionCache.serverUrl.toHttpUrl()
        } catch (e: IllegalArgumentException) {
            throw IOException("Indirizzo server non valido: ${sessionCache.serverUrl}", e)
        }

        val newUrl = original.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()

        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
