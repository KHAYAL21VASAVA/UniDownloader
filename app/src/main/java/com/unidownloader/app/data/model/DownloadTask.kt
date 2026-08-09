package com.unidownloader.app.data.model

import java.util.UUID

enum class DownloadStatus {
    QUEUED,
    FETCHING_STREAMS,
    DOWNLOADING_VIDEO,
    DOWNLOADING_AUDIO,
    DOWNLOADING_DIRECT,
    MERGING,
    FINALIZING,
    SAVING_TO_STORAGE,
    COMPLETED,
    PAUSED,
    CANCELLED,
    FAILED
}

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val mediaTitle: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val selectedVideoQuality: String = "Best",
    val selectedAudioQuality: String = "Best",
    val selectedContainerFormat: String = "MP4",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progressPercent: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val stageDescription: String = "Queued",
    val outputFileName: String = "",
    val outputFilePath: String? = null,
    val outputUriString: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isFinished: Boolean
        get() = status == DownloadStatus.COMPLETED || status == DownloadStatus.CANCELLED || status == DownloadStatus.FAILED

    val isActive: Boolean
        get() = !isFinished && status != DownloadStatus.PAUSED
}
