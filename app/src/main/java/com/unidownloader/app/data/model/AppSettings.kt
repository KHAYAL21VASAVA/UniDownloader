package com.unidownloader.app.data.model

data class AppSettings(
    val defaultVideoQuality: String = "Best",
    val defaultAudioQuality: String = "Best",
    val defaultFormat: String = "MP4",
    val downloadNotificationsEnabled: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val maxConcurrentDownloads: Int = 3,
    val autoClearCompleted: Boolean = false
)
