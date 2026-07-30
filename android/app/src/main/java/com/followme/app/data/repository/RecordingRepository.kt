package com.followme.app.data.repository

import com.followme.app.data.local.SessionCache
import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.RecordingApi
import com.followme.app.data.remote.apiCall
import com.followme.app.data.remote.dto.RecordingDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

class RecordingRepository(
    private val recordingApi: RecordingApi,
    private val sessionCache: SessionCache,
) {
    suspend fun listRecordings(deviceId: String, limit: Int = 20, offset: Int = 0): ApiResult<List<RecordingDto>> =
        when (val result = apiCall { recordingApi.listRecordings(deviceId, limit, offset) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.recordings)
            is ApiResult.Failure -> result
        }

    suspend fun deleteRecording(recordingId: String): ApiResult<Unit> =
        when (val result = apiCall { recordingApi.deleteRecording(recordingId) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }

    /** Streams the recording's bytes into [sink] (already opened by the caller, e.g. a MediaStore Downloads entry). Closes [sink] when done, on success or failure. */
    suspend fun downloadTo(recordingId: String, sink: OutputStream): ApiResult<Unit> {
        return when (val result = apiCall { recordingApi.download(recordingId) }) {
            is ApiResult.Success -> {
                try {
                    withContext(Dispatchers.IO) {
                        result.data.byteStream().use { input -> sink.use { output -> input.copyTo(output) } }
                    }
                    ApiResult.Success(Unit)
                } catch (e: IOException) {
                    ApiResult.Failure("Errore durante il download: ${e.message ?: "errore I/O"}")
                }
            }
            is ApiResult.Failure -> {
                sink.close()
                result
            }
        }
    }

    /** Absolute URL usable directly by ExoPlayer (via the shared authenticated OkHttpClient). */
    fun streamingUrl(recordingId: String): String =
        "${sessionCache.serverUrl}/api/recordings/$recordingId/download"
}
