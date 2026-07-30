package com.followme.app.data.remote

import com.followme.app.data.remote.dto.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String, val httpCode: Int? = null) : ApiResult<Nothing>()
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Wraps a Retrofit call, turning HTTP error bodies and network exceptions into a friendly [ApiResult.Failure]. */
suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body)
        } else {
            ApiResult.Failure(parseErrorMessage(response), response.code())
        }
    } catch (e: IOException) {
        ApiResult.Failure("Impossibile raggiungere il server: ${e.message ?: "errore di rete"}")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Errore imprevisto")
    }
}

private fun <T> parseErrorMessage(response: Response<T>): String {
    val errorBody = response.errorBody()?.string()
    if (!errorBody.isNullOrBlank()) {
        runCatching { errorJson.decodeFromString(ErrorResponse.serializer(), errorBody) }
            .getOrNull()
            ?.let { return it.error }
    }
    return "Richiesta fallita (HTTP ${response.code()})"
}
