package com.followme.app.data.remote

import com.followme.app.data.remote.dto.ChunkUploadResponse
import com.followme.app.data.remote.dto.OkResponse
import com.followme.app.data.remote.dto.RecordingCompleteRequest
import com.followme.app.data.remote.dto.RecordingInitRequest
import com.followme.app.data.remote.dto.RecordingInitResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** Called by the camera role (device-token authenticated, see AuthHeaderInterceptor) to upload finished recording segments. */
interface DeviceRecordingApi {
    @POST("api/device/recordings")
    suspend fun initRecording(@Body body: RecordingInitRequest): Response<RecordingInitResponse>

    @POST("api/device/recordings/{id}/chunk")
    suspend fun uploadChunk(@Path("id") recordingId: String, @Body body: RequestBody): Response<ChunkUploadResponse>

    @POST("api/device/recordings/{id}/complete")
    suspend fun completeRecording(
        @Path("id") recordingId: String,
        @Body body: RecordingCompleteRequest,
    ): Response<OkResponse>

    @POST("api/device/recordings/{id}/fail")
    suspend fun failRecording(@Path("id") recordingId: String): Response<OkResponse>
}
