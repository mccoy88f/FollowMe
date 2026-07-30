package com.followme.app.camera

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object CameraNotifications {
    const val CHANNEL_ID = "camera_service"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Modalità videocamera",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Notifica persistente mentre questo telefono funziona da videocamera remota"
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, contentText: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("FollowMe - Videocamera")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
