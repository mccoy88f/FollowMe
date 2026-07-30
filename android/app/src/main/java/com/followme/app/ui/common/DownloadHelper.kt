package com.followme.app.ui.common

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

/** Opens a writable stream for a new file in the public Downloads collection, using MediaStore on API 29+ (scoped storage, no permission needed) or a direct file path pre-29 (requires WRITE_EXTERNAL_STORAGE, requested by the caller). */
object DownloadHelper {
    fun openDownloadsOutputStream(context: Context, fileName: String, mimeType: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            val stream = resolver.openOutputStream(uri) ?: return null

            object : OutputStream() {
                override fun write(b: Int) = stream.write(b)
                override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)
                override fun flush() = stream.flush()
                override fun close() {
                    stream.close()
                    val donePending = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                    resolver.update(uri, donePending, null, null)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            File(downloadsDir, fileName).outputStream()
        }
    }
}
