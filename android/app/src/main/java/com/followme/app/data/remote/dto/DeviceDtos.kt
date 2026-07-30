package com.followme.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val id: String,
    val name: String,
    val paired: Boolean,
    val status: String,
    val online: Boolean,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class DeviceListResponse(val devices: List<DeviceDto>)

@Serializable
data class CreateDeviceRequest(val name: String)

@Serializable
data class DeviceRef(val id: String, val name: String)

@Serializable
data class CreateDeviceResponse(
    val device: DeviceRef,
    val pairingToken: String,
    val pairingTokenExpiresAt: String,
)

@Serializable
data class RegeneratePairingResponse(
    val pairingToken: String,
    val pairingTokenExpiresAt: String,
)

@Serializable
data class CommandRequest(val action: String, val type: String? = null)

@Serializable
data class CommandResponse(val delivered: Boolean, val reason: String? = null)

@Serializable
data class PairDeviceRequest(val pairingToken: String)

@Serializable
data class PairDeviceResponse(val deviceToken: String, val deviceId: String, val name: String)
