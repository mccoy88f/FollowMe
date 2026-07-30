package com.followme.app.data.remote

import com.followme.app.data.remote.dto.OkResponse
import com.followme.app.data.remote.dto.RecordingListResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface RecordingApi {
    @GET("api/recordings")
    suspend fun listRecordings(
        @Query("deviceId") deviceId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<RecordingListResponse>

    @DELETE("api/recordings/{id}")
    suspend fun deleteRecording(@Path("id") recordingId: String): Response<OkResponse>

    @Streaming
    @GET("api/recordings/{id}/download")
    suspend fun download(@Path("id") recordingId: String): Response<ResponseBody>
}
