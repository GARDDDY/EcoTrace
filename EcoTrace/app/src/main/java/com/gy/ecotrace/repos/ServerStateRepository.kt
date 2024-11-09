package com.gy.ecotrace.repos

import com.gy.ecotrace.network.ServerApi
import javax.inject.Inject

class ServerStateRepository @Inject constructor(
    private val api: ServerApi,
) {
    suspend fun pingServer(): Boolean {
        return try {
            val response = api.ping()
            // Drain the body so OkHttp can recycle the connection.
            response.body()?.close()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}