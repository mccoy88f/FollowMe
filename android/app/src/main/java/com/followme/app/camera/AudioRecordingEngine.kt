package com.followme.app.camera

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Mic-only recording (no camera involved) using MediaRecorder, writing raw ADTS AAC - a self-contained, frame-based format safe to treat as complete standalone files per segment. */
class AudioRecordingEngine(private val context: Context) : RecordingEngine {
    override val recordingType = "audio"
    override val fileExtension = "aac"
    override val mimeType = "audio/aac"

    private var recorder: MediaRecorder? = null
    private var startTimeMs: Long = 0

    override suspend fun prepare() {
        // Nothing to pre-bind; a MediaRecorder is created fresh per segment
        // since it cannot be reused after stop()+release().
    }

    override suspend fun startSegment(outputFile: File) {
        val mr = newMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        startTimeMs = System.currentTimeMillis()
    }

    override suspend fun stopSegment(): Int {
        val mr = recorder ?: return 0
        val durationSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
        try {
            mr.stop()
        } catch (e: RuntimeException) {
            // Thrown if stop() is called with essentially no audio captured
            // (e.g. immediate stop command); the segment is just discarded
            // upstream if the resulting file turns out empty.
        }
        mr.release()
        recorder = null
        return durationSeconds
    }

    override fun release() {
        recorder?.release()
        recorder = null
    }

    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
