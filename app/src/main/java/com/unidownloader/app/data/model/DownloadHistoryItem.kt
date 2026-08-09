package com.unidownloader.app.data.model

import java.util.UUID

data class DownloadHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val fileName: String,
    val extension: String,
    val fileSizeBytes: Long,
    val filePath: String,
    val fileUriString: String,
    val mimeType: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val sourceUrl: String = ""
)
