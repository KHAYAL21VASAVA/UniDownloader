package com.unidownloader.app.utils

import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {

    private val decFormat = DecimalFormat("#,##0.00")
    private val singleDecFormat = DecimalFormat("#,##0.0")

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return "${singleDecFormat.format(value)} ${units[index]}"
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0.0 KB/s"
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00:00"
        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0 || seconds > 86400) return "ETA: --:--"
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "ETA: %02d:%02d", minutes, secs)
    }

    fun formatDate(timestampMs: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        return sdf.format(java.util.Date(timestampMs))
    }

    fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase().trimStart('.')) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            else -> "application/octet-stream"
        }
    }
}
