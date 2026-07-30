package com.followme.app.data.socket

data class DeviceStatusEvent(val deviceId: String, val online: Boolean)

data class DeviceRecordingStatusEvent(val deviceId: String, val state: String?)
