package com.followme.app.data.socket

import com.followme.app.data.local.SessionCache
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.URI

data class DeviceCommand(val action: String, val type: String?)

/**
 * Socket.IO connection used by the camera role to receive start/stop
 * commands and report its own status/presence (see
 * backend/README.md "Comandi realtime", role=device side).
 */
class DeviceSocketClient(private val sessionCache: SessionCache) {

    private var socket: Socket? = null

    private val _commands = MutableSharedFlow<DeviceCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<DeviceCommand> = _commands

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun connect() {
        if (socket?.connected() == true) return

        val token = sessionCache.deviceToken ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("role" to "device", "token" to token))
            .build()

        val newSocket = try {
            IO.socket(URI.create(sessionCache.serverUrl), options)
        } catch (e: IllegalArgumentException) {
            return
        }

        newSocket.on(Socket.EVENT_CONNECT) { _isConnected.value = true }
        newSocket.on(Socket.EVENT_DISCONNECT) { _isConnected.value = false }

        newSocket.on("command") { args ->
            (args.getOrNull(0) as? JSONObject)?.let { json ->
                val action = json.optString("action")
                val type = if (json.has("type") && !json.isNull("type")) json.optString("type") else null
                if (action.isNotEmpty()) {
                    _commands.tryEmit(DeviceCommand(action, type))
                }
            }
        }

        newSocket.connect()
        socket = newSocket
    }

    fun emitStatus(state: String, extra: Map<String, String> = emptyMap()) {
        val json = JSONObject().put("state", state)
        extra.forEach { (key, value) -> json.put(key, value) }
        socket?.emit("status", json)
    }

    fun emitHeartbeat() {
        socket?.emit("heartbeat")
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        _isConnected.value = false
    }
}
