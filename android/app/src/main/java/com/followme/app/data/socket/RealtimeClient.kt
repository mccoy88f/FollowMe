package com.followme.app.data.socket

import com.followme.app.data.local.SessionCache
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject
import java.net.URI

/**
 * Wraps the Socket.IO connection used by the controller app to receive
 * realtime device presence and recording-state updates (see
 * backend/README.md "Comandi realtime"). Connects with role=user using the
 * current access token.
 *
 * Simplification: the token is read once at [connect] time. Since this app
 * reconnects per screen visit (see DeviceListViewModel), a stale token only
 * matters across very long-lived single sessions, which is an acceptable
 * trade-off for phase 2 - revisit if that turns out to be an issue.
 */
class RealtimeClient(private val sessionCache: SessionCache) {

    private var socket: Socket? = null

    private val _deviceStatus = MutableSharedFlow<DeviceStatusEvent>(extraBufferCapacity = 16)
    val deviceStatus: SharedFlow<DeviceStatusEvent> = _deviceStatus

    private val _recordingStatus = MutableSharedFlow<DeviceRecordingStatusEvent>(extraBufferCapacity = 16)
    val recordingStatus: SharedFlow<DeviceRecordingStatusEvent> = _recordingStatus

    fun connect() {
        if (socket?.connected() == true) return

        val token = sessionCache.accessToken ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("role" to "user", "token" to token))
            .build()

        val newSocket = try {
            IO.socket(URI.create(sessionCache.serverUrl), options)
        } catch (e: IllegalArgumentException) {
            // Malformed server URL: skip realtime connection rather than
            // crash: REST calls will still surface the problem to the user.
            return
        }

        newSocket.on("device_status") { args ->
            (args.getOrNull(0) as? JSONObject)?.let { json ->
                _deviceStatus.tryEmit(
                    DeviceStatusEvent(
                        deviceId = json.optString("deviceId"),
                        online = json.optString("status") == "online",
                    )
                )
            }
        }

        newSocket.on("device_recording_status") { args ->
            (args.getOrNull(0) as? JSONObject)?.let { json ->
                _recordingStatus.tryEmit(
                    DeviceRecordingStatusEvent(
                        deviceId = json.optString("deviceId"),
                        state = if (json.has("state")) json.optString("state") else null,
                    )
                )
            }
        }

        newSocket.connect()
        socket = newSocket
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }
}
