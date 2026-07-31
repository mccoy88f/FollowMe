package com.followme.app.data.repository

import com.followme.app.data.remote.ApiResult
import com.followme.app.data.remote.DeviceRecordingApi
import com.followme.app.data.remote.apiCall
import com.followme.app.data.remote.dto.RecordingCompleteRequest
import com.followme.app.data.remote.dto.RecordingInitRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/** Uploads finished local recording segments (see camera/RecordingEngine) to the backend, device-token authenticated. */
class DeviceRecordingRepository(private val deviceRecordingApi: DeviceRecordingApi) {

    /** Runs the whole init -> upload -> complete sequence for one finished segment file. The file is left on local storage either way (kept as the "copy on the camera device" the user asked for). */
    suspend fun uploadSegment(file: File, type: String, mimeType: String, durationSeconds: Int): ApiResult<String> {
        val initResult = apiCall { deviceRecordingApi.initRecording(RecordingInitRequest(type)) }
        val recordingId = when (initResult) {
            is ApiResult.Success -> initResult.data.recordingId
            is ApiResult.Failure -> return initResult
        }

        val chunkResult = withContext(Dispatchers.IO) {
            apiCall {
                val mediaType = (if (mimeType.isNotBlank()) mimeType else "application/octet-stream").toMediaType()
                val body = file.asRequestBody(mediaType)
                deviceRecordingApi.uploadChunk(recordingId, body)
            }
        }
        if (chunkResult is ApiResult.Failure) {
            runCatching { deviceRecordingApi.failRecording(recordingId) }
            return chunkResult
        }

        val completeResult = apiCall {
            deviceRecordingApi.completeRecording(recordingId, RecordingCompleteRequest(durationSeconds))
        }
        return when (completeResult) {
            is ApiResult.Success -> ApiResult.Success(recordingId)
            is ApiResult.Failure -> completeResult
        }
    }
}
