package com.followme.app.camera

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.followme.app.AppContainer
import com.followme.app.FollowMeApp
import com.followme.app.data.socket.DeviceCommand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CameraServiceState(
    val running: Boolean = false,
    val connected: Boolean = false,
    val recording: Boolean = false,
    val lastError: String? = null,
)

/**
 * Runs while this device is acting as a remote camera: keeps a Socket.IO
 * connection open (role=device), waits for start/stop commands, and drives
 * fixed-length recording segments (see [RecordingEngine]) that are uploaded
 * to the backend as soon as each one finishes.
 *
 * Always shows a persistent notification while running (both a foreground
 * service requirement on modern Android and, by design, never hidden - see
 * the project's README on why covert recording is out of scope).
 */
class CameraForegroundService : LifecycleService() {

    private lateinit var container: AppContainer
    private var recordingJob: Job? = null
    private val stopRequested = MutableStateFlow(false)

    // A foreground service alone does not keep the CPU awake once the
    // screen turns off - without this, recording (and the WebSocket
    // connection needed to even receive commands) can stall as soon as the
    // device goes to standby. Renewed periodically on each heartbeat tick
    // instead of held with no timeout, so a bug here can't drain the
    // battery indefinitely if release() is ever missed.
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as FollowMeApp).container
        CameraNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopSelfService()
            return START_NOT_STICKY
        }

        startAsForeground()
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = CameraNotifications.build(this, "In attesa di comandi")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                CameraNotifications.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(CameraNotifications.NOTIFICATION_ID, notification)
        }

        if (_state.value.running) return // already started (e.g. duplicate start command)
        _state.value = _state.value.copy(running = true)

        acquireWakeLock()
        container.deviceSocketClient.connect()

        lifecycleScope.launch {
            container.deviceSocketClient.isConnected.collect { connected ->
                _state.value = _state.value.copy(connected = connected)
            }
        }
        lifecycleScope.launch {
            container.deviceSocketClient.commands.collect { command -> handleCommand(command) }
        }
        lifecycleScope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                container.deviceSocketClient.emitHeartbeat()
                acquireWakeLock() // renew before the previous timeout would expire
            }
        }
    }

    private fun acquireWakeLock() {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FollowMe:cameraRecording")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun handleCommand(command: DeviceCommand) {
        when (command.action) {
            "start" -> {
                val type = command.type ?: return
                if (recordingJob?.isActive == true) return
                stopRequested.value = false
                recordingJob = lifecycleScope.launch { runRecordingLoop(type) }
            }
            "stop" -> {
                stopRequested.value = true
            }
        }
    }

    private fun hasRequiredPermissions(type: String): Boolean {
        val recordAudioGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (type == "audio") return recordAudioGranted

        val cameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return cameraGranted && (type != "audio_video" || recordAudioGranted)
    }

    private suspend fun runRecordingLoop(type: String) {
        if (!hasRequiredPermissions(type)) {
            container.deviceSocketClient.emitStatus("error", mapOf("message" to "Permessi mancanti sul dispositivo"))
            _state.value = _state.value.copy(lastError = "Permessi mancanti per: $type")
            return
        }

        val engine: RecordingEngine = when (type) {
            "audio" -> AudioRecordingEngine(this)
            "video" -> VideoRecordingEngine(this, this, includeAudio = false)
            "audio_video" -> VideoRecordingEngine(this, this, includeAudio = true)
            else -> return
        }

        try {
            engine.prepare()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            container.deviceSocketClient.emitStatus("error", mapOf("message" to (e.message ?: "prepare failed")))
            _state.value = _state.value.copy(lastError = e.message)
            return
        }

        _state.value = _state.value.copy(recording = true, lastError = null)
        container.deviceSocketClient.emitStatus("recording_started")
        updateNotification("Registrazione in corso")

        // getExternalFilesDir can return null if shared storage is briefly
        // unavailable; fall back to internal storage rather than crash.
        val baseDir = getExternalFilesDir(null) ?: filesDir
        val recordingsDir = File(baseDir, "recordings").apply { mkdirs() }

        try {
            while (!stopRequested.value) {
                val fileName = "${SEGMENT_DATE_FORMAT.format(Date())}.${engine.fileExtension}"
                val segmentFile = File(recordingsDir, fileName)

                try {
                    engine.startSegment(segmentFile)
                    withTimeoutOrNull(SEGMENT_DURATION_MS) {
                        stopRequested.filter { it }.first()
                    }
                    val durationSeconds = engine.stopSegment()

                    if (segmentFile.exists() && segmentFile.length() > 0) {
                        lifecycleScope.launch {
                            container.deviceRecordingRepository.uploadSegment(
                                file = segmentFile,
                                type = engine.recordingType,
                                mimeType = engine.mimeType,
                                durationSeconds = durationSeconds,
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // A single failed segment (e.g. disk full, camera error)
                    // ends the loop cleanly instead of crashing the service -
                    // an always-on background component must never let a
                    // transient recording error take the whole process down.
                    container.deviceSocketClient.emitStatus("error", mapOf("message" to (e.message ?: "recording failed")))
                    _state.value = _state.value.copy(lastError = e.message)
                    break
                }
            }
        } finally {
            engine.release()
            _state.value = _state.value.copy(recording = false)
            container.deviceSocketClient.emitStatus("recording_stopped")
            updateNotification("In attesa di comandi")
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(CameraNotifications.NOTIFICATION_ID, CameraNotifications.build(this, text))
    }

    private fun stopSelfService() {
        stopRequested.value = true
        container.deviceSocketClient.disconnect()
        releaseWakeLock()
        _state.value = CameraServiceState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        container.deviceSocketClient.disconnect()
        releaseWakeLock()
        _state.value = CameraServiceState()
        super.onDestroy()
    }

    // Deliberately does nothing beyond the default: a started foreground
    // service is independent of the launching Activity's task, so swiping
    // FollowMe away from Recents must not stop the camera. Overridden
    // explicitly (rather than left to the inherited default) so that stays
    // true regardless of what LifecycleService's own implementation does.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        private const val ACTION_STOP = "com.followme.app.camera.STOP"
        private const val SEGMENT_DURATION_MS = 30_000L
        private const val HEARTBEAT_INTERVAL_MS = 25_000L
        // Must be comfortably longer than HEARTBEAT_INTERVAL_MS, which renews it.
        private const val WAKE_LOCK_TIMEOUT_MS = 5 * 60_000L
        private val SEGMENT_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

        private val _state = MutableStateFlow(CameraServiceState())
        val state: StateFlow<CameraServiceState> = _state

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CameraForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CameraForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
