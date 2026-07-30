package com.followme.app.data.remote

import com.followme.app.data.remote.dto.CommandRequest
import com.followme.app.data.remote.dto.CommandResponse
import com.followme.app.data.remote.dto.CreateDeviceRequest
import com.followme.app.data.remote.dto.CreateDeviceResponse
import com.followme.app.data.remote.dto.DeviceListResponse
import com.followme.app.data.remote.dto.OkResponse
import com.followme.app.data.remote.dto.RegeneratePairingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceApi {
    @GET("api/devices")
    suspend fun listDevices(): Response<DeviceListResponse>

    @POST("api/devices")
    suspend fun createDevice(@Body body: CreateDeviceRequest): Response<CreateDeviceResponse>

    @POST("api/devices/{id}/regenerate-pairing-token")
    suspend fun regeneratePairingToken(@Path("id") deviceId: String): Response<RegeneratePairingResponse>

    @DELETE("api/devices/{id}")
    suspend fun deleteDevice(@Path("id") deviceId: String): Response<OkResponse>

    @POST("api/devices/{id}/command")
    suspend fun sendCommand(
        @Path("id") deviceId: String,
        @Body body: CommandRequest,
    ): Response<CommandResponse>
}
