package com.followme.app.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Signals that the session was invalidated (refresh token expired/revoked) so the UI can force navigation back to the login screen. */
class AuthEventBus {
    private val _loggedOut = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val loggedOut: SharedFlow<Unit> = _loggedOut

    fun notifyLoggedOut() {
        _loggedOut.tryEmit(Unit)
    }
}
