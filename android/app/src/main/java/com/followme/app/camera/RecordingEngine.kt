package com.followme.app.camera

import java.io.File

/** One concrete recording technology (camera+mic, or mic-only). The foreground service drives a sequence of fixed-length segments through the same engine instance: prepare() once, then startSegment()/stopSegment() repeatedly. */
interface RecordingEngine {
    val recordingType: String
    val fileExtension: String
    val mimeType: String

    /** One-time setup (e.g. bind the camera). Call before the first [startSegment]. */
    suspend fun prepare()

    /** Begins writing a new segment to [outputFile]. */
    suspend fun startSegment(outputFile: File)

    /** Finalizes the current segment (safe to read/upload [outputFile] only after this returns) and returns its duration in seconds. */
    suspend fun stopSegment(): Int

    /** Releases the camera/recorder for good; the engine cannot be reused after this. */
    fun release()
}
