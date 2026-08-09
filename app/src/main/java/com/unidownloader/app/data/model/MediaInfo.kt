package com.unidownloader.app.data.model

data class MediaInfo(
    val url: String,
    val title: String,
    val uploader: String = "Unknown Creator",
    val thumbnail: String? = null,
    val durationSeconds: Long = 0L,
    val sourcePlatform: String = "Web Media",
    val formats: List<MediaFormat> = emptyList(),
    val description: String? = null,
    val extractorName: String = "generic",
    val originalFilename: String = "",
    val approximateSizeBytes: Long = 0L
) {
    /**
     * Extracts available unique video quality tiers from the parsed formats
     */
    fun getAvailableVideoQualities(): List<String> {
        val qualities = mutableListOf("Best")
        val availableLabels = formats
            .filter { it.isVideo && !it.isAudioOnly }
            .map { it.displayResolution }
            .distinct()

        val priorityOrder = listOf(
            "2160p (4K)",
            "1440p (2K)",
            "1080p (Full HD)",
            "720p (HD)",
            "480p",
            "360p",
            "240p"
        )

        for (p in priorityOrder) {
            if (availableLabels.contains(p)) {
                qualities.add(p)
            }
        }

        // Add any other custom resolution label if found
        for (label in availableLabels) {
            if (!qualities.contains(label) && label != "Audio only") {
                qualities.add(label)
            }
        }

        qualities.add("Audio only")
        return qualities
    }

    /**
     * Extracts available audio bitrates
     */
    fun getAvailableAudioQualities(): List<String> {
        val qualities = mutableListOf("Best")
        val availableLabels = formats
            .filter { it.isAudioOnly || it.abr > 0 || it.isMuxed }
            .map { it.displayAudioBitrate }
            .distinct()

        val standardRates = listOf("320 kbps", "256 kbps", "192 kbps", "128 kbps", "Original")
        for (rate in standardRates) {
            if (!qualities.contains(rate)) {
                qualities.add(rate)
            }
        }
        return qualities
    }

    /**
     * Extracts valid container formats based on media type
     */
    fun getSupportedContainers(isVideo: Boolean): List<String> {
        return if (isVideo) {
            listOf("MP4", "MKV", "WEBM")
        } else {
            listOf("MP3", "M4A", "OPUS")
        }
    }
}
