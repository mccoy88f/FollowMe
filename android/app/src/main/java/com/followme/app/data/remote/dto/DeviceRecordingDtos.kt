package com.followme.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecordingInitRequest(val type: String)

@Serializable
data class RecordingInitResponse(val recordingId: String)

@Serializable
data class RecordingCompleteRequest(val durationSeconds: Int? = null)

/** Note: unlike [RecordingDto.bytesReceived] (a Postgres bigint serialized as a string), this comes straight from a JS number in the chunk-upload response - a real JSON number. */
@Serializable
data class ChunkUploadResponse(val bytesReceived: Long)
