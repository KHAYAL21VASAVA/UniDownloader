package com.unidownloader.app

import android.app.Application
import android.util.Log
import com.unidownloader.app.data.repository.DownloadRepository
import com.unidownloader.app.data.repository.SettingsRepository
import com.unidownloader.app.download.DownloadManager
import com.unidownloader.app.ffmpeg.FFmpegManager
import com.unidownloader.app.notification.NotificationManager
import com.unidownloader.app.python.PythonBridge
import com.unidownloader.app.storage.DownloadStorage
import com.yausername.youtubedl_android.YoutubeDL

class UniDownloaderApp : Application() {

    companion object {
        private const val TAG = "UniDownloaderApp"
        lateinit var instance: UniDownloaderApp
            private set

        lateinit var downloadRepository: DownloadRepository
            private set

        lateinit var settingsRepository: SettingsRepository
            private set

        lateinit var downloadStorage: DownloadStorage
            private set

        lateinit var ffmpegManager: FFmpegManager
            private set

        lateinit var pythonBridge: PythonBridge
            private set

        lateinit var downloadManager: DownloadManager
            private set
    }

    private var isYoutubeDlInitialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Notification Channels
        NotificationManager.createNotificationChannels(this)

        // 2. Initialize Repositories & Storage
        settingsRepository = SettingsRepository(this)
        downloadStorage = DownloadStorage(this)
        downloadRepository = DownloadRepository(this, downloadStorage)
        ffmpegManager = FFmpegManager(this)
        pythonBridge = PythonBridge(this)
        downloadManager = DownloadManager(
            context = this,
            repository = downloadRepository,
            storage = downloadStorage,
            ffmpegManager = ffmpegManager,
            settingsRepository = settingsRepository
        )
        downloadStorage.cleanupStaleTempFiles()

        // 3. Initialize Python / YoutubeDL / FFmpeg engines
        try {
            YoutubeDL.getInstance().init(this)
            com.yausername.ffmpeg.FFmpeg.getInstance().init(this)
            isYoutubeDlInitialized = true
            Log.i(TAG, "Native Python & FFmpeg engines successfully initialized")
        } catch (e: Throwable) {
            Log.w(TAG, "Native engine initialization note: ${e.message}")
            isYoutubeDlInitialized = false
        }
    }

    fun isEngineReady(): Boolean = isYoutubeDlInitialized
}
