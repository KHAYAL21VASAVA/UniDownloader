package com.unidownloader.app.data.model

data class MediaFormat(
    val formatId: String,
    val ext: String = "mp4",
    val resolution: String = "Unknown",
    val height: Int = 0,
    val width: Int = 0,
    val fps: Int = 0,
    val vcodec: String? = null,
    val acodec: String? = null,
    val abr: Double = 0.0,
    val tbr: Double = 0.0,
    val filesize: Long = 0L,
    val formatNote: String? = null,
    val downloadUrl: String? = null,
    val isVideo: Boolean = true,
    val isAudioOnly: Boolean = false,
    val isMuxed: Boolean = false
) {
    val displayResolution: String
        get() {
            if (isAudioOnly) return "Audio only"
            return when {
                height >= 2160 || resolution.contains("2160") || resolution.contains("4k", ignoreCase = true) -> "2160p (4K)"
                height >= 1440 || resolution.contains("1440") || resolution.contains("2k", ignoreCase = true) -> "1440p (2K)"
                height >= 1080 || resolution.contains("1080") -> "1080p (Full HD)"
                height >= 720 || resolution.contains("720") -> "720p (HD)"
                height >= 480 || resolution.contains("480") -> "480p"
                height >= 360 || resolution.contains("360") -> "360p"
                height >= 240 || resolution.contains("240") -> "240p"
                height > 0 -> "${height}p"
                resolution.isNotBlank() && resolution != "Unknown" -> resolution
                else -> "Best"
            }
        }

    val displayAudioBitrate: String
        get() {
            return when {
                abr >= 300 || tbr >= 300 -> "320 kbps"
                abr >= 240 || tbr >= 240 -> "256 kbps"
                abr >= 180 || tbr >= 180 -> "192 kbps"
                abr >= 120 || tbr >= 120 -> "128 kbps"
                abr >= 64 || tbr >= 64 -> "96 kbps"
                abr > 0 -> "${abr.toInt()} kbps"
                else -> "Original"
            }
        }
}
