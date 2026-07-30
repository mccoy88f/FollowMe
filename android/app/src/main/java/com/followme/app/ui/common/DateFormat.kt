package com.followme.app.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val displayFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())

/** Formats a backend ISO-8601 timestamp (e.g. "2026-07-30T06:24:16.188Z") for display, or a placeholder if null/unparseable. */
fun formatTimestamp(iso: String?, placeholder: String = "-"): String {
    if (iso.isNullOrBlank()) return placeholder
    return runCatching { displayFormatter.format(Instant.parse(iso)) }.getOrDefault(placeholder)
}

fun formatDuration(seconds: Int?): String {
    if (seconds == null) return "-"
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "%d:%02d".format(m, s) else "%ds".format(s)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = -1
    do {
        value /= 1024
        unitIndex++
    } while (value >= 1024 && unitIndex < units.size - 1)
    return "%.1f %s".format(value, units[unitIndex])
}
