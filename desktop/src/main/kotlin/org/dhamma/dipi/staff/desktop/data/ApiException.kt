package org.dhamma.dipi.staff.desktop.data

import retrofit2.HttpException
import java.io.IOException

class ApiException(
    message: String,
    val unauthorized: Boolean = false,
) : RuntimeException(message)

fun Throwable.toApi(): ApiException = when (this) {
    is ApiException -> this
    is HttpException -> {
        val raw = response()?.errorBody()?.string().orEmpty()
        val msg = MSG.find(raw)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?: (message() ?: "Request failed")
        ApiException(msg, code() == 401 || code() == 403)
    }
    is IOException -> ApiException("Offline")
    else -> ApiException(message ?: "Request failed")
}

private val MSG = Regex("\"msg\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
