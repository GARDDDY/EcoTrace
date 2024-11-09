package com.gy.ecotrace.network

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

data class ApiResponse<T>(
    val ok: Boolean = false,
    val data: T? = null,
    val error: String? = null,
    val details: String? = null
)

class ApiException(
    val httpCode: Int? = null,
    message: String?
) : Exception(message ?: "Неизвестная ошибка сервера")

private data class ErrorEnvelope(val error: String? = null)

suspend fun <T> apiCall(block: suspend () -> ApiResponse<T>): T {
    val response = try {
        block()
    } catch (e: HttpException) {
        throw e.toApiException()
    } catch (e: IOException) {
        throw ApiException(message = "Нет соединения с сервером")
    }

    if (!response.ok) throw ApiException(message = response.error)
    return response.data
        ?: throw ApiException(message = response.error ?: "Пустой ответ сервера")
}

fun HttpException.toApiException(): ApiException {
    val raw = response()?.errorBody()?.string()
    val parsed = raw?.let {
        runCatching { Gson().fromJson(it, ErrorEnvelope::class.java) }.getOrNull()
    }
    return ApiException(httpCode = code(), message = parsed?.error ?: message())
}
