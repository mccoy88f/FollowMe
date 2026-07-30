package com.followme.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordingDto(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    val type: String,
    val status: String,
    // Postgres bigint is serialized as a JSON string by the backend to avoid
    // precision loss; parse with toLongOrNull() where a number is needed.
    @SerialName("bytes_received") val bytesReceived: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class RecordingListResponse(val recordings: List<RecordingDto>)

@Serializable
data class RecordingResponse(val recording: RecordingDto)
