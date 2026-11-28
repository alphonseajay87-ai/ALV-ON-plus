package com.alveon.plus.data

import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/** Adds HTTP Basic auth, but only for requests to the exact host/port of a saved server. */
class ServerAuthInterceptor(private val servers: () -> List<ServerEntry>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("Authorization") != null) return chain.proceed(request)
        val match = servers().firstOrNull { s ->
            s.username.isNotBlank() && s.url.toHttpUrlOrNull()?.let {
                it.host == request.url.host && it.port == request.url.port
            } == true
        } ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header("Authorization", Credentials.basic(match.username, match.password)).build()
        )
    }
}
