package com.gy.ecotrace.network

import android.util.Log
import com.gy.ecotrace.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class NetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) return chain.proceed(chain.request())

        val request = chain.request()
        val label = "${request.method} ${request.url.encodedPath}"
        val start = System.currentTimeMillis()
        Log.d(TAG, "[SEND] $label")

        return try {
            val response = chain.proceed(request)
            val elapsed = System.currentTimeMillis() - start
            if (response.isSuccessful) {
                Log.d(TAG, "[RECV] ${response.code} $label (${elapsed}ms)")
            } else {
                Log.w(TAG, "[RECV] ${response.code} $label (${elapsed}ms) — ${response.message}")
            }
            response
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - start
            Log.e(TAG, "[ERRO] $label (${elapsed}ms) — ${e.message}")
            throw e
        }
    }

    companion object {
        private const val TAG = "ETNetwork"
    }
}
