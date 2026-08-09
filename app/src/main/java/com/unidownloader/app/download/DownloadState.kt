package com.unidownloader.app.download

data class DownloadState(
    val id: String,
    val url: String,
    val filename: String,
    val status: String = "QUEUED", // QUEUED, DOWNLOADING, PROCESSING, COMPLETED, FAILED, CANCELLED
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speed: Long = 0L,
    val eta: Long = 0L,
    val stage: String = "Preparing...",
    val outputPath: String? = null,
    val error: String? = null
) {
    val isActive: Boolean
        get() = status == "QUEUED" || status == "DOWNLOADING" || status == "PROCESSING"

    val isCompleted: Boolean
        get() = status == "COMPLETED"

    val isFailed: Boolean
        get() = status == "FAILED"
}
