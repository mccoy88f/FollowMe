package com.followme.app.camera

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * Video (optionally with audio) recording via CameraX's Recorder API. One
 * [ProcessCameraProvider]/[VideoCapture] is bound in [prepare] and reused
 * across all segments; each [startSegment]/[stopSegment] pair drives one
 * finalized MP4 file.
 */
class VideoRecordingEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val includeAudio: Boolean,
) : RecordingEngine {
    override val recordingType = if (includeAudio) "audio_video" else "video"
    override val fileExtension = "mp4"
    override val mimeType = "video/mp4"

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var finalizeDeferred: CompletableDeferred<Boolean>? = null
    private var startTimeMs: Long = 0

    override suspend fun prepare() {
        val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                { cont.resume(future.get()) },
                ContextCompat.getMainExecutor(context),
            )
        }
        cameraProvider = provider

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))
            )
            .build()
        val capture = VideoCapture.withOutput(recorder)
        videoCapture = capture

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, capture)
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO/CAMERA are checked by the foreground service before recording starts
    override suspend fun startSegment(outputFile: File) {
        val capture = videoCapture ?: error("VideoRecordingEngine not prepared")
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        val deferred = CompletableDeferred<Boolean>()
        finalizeDeferred = deferred

        var pending = capture.output.prepareRecording(context, outputOptions)
        if (includeAudio) {
            pending = pending.withAudioEnabled()
        }

        startTimeMs = System.currentTimeMillis()
        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                deferred.complete(!event.hasError())
            }
        }
    }

    override suspend fun stopSegment(): Int {
        val durationSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
        activeRecording?.stop()
        // Bounded wait: if CameraX never delivers the Finalize event (e.g. a
        // camera driver failure), don't hang the recording loop forever -
        // move on and let the next segment attempt surface the problem.
        withTimeoutOrNull(FINALIZE_TIMEOUT_MS) { finalizeDeferred?.await() }
        activeRecording = null
        finalizeDeferred = null
        return durationSeconds
    }

    override fun release() {
        activeRecording?.close()
        activeRecording = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoCapture = null
    }

    companion object {
        private const val FINALIZE_TIMEOUT_MS = 5_000L
    }
}
