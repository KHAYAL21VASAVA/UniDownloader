package com.unidownloader.app.download

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.unidownloader.app.UniDownloaderApp
import com.unidownloader.app.notification.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {

    companion object {
        private const val SERVICE_NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationManager.createNotificationChannels(this)
        startForegroundWithDefaultNotification()
        observeActiveTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun startForegroundWithDefaultNotification() {
        val notification = NotificationManager.buildProgressNotification(
            context = this,
            title = "Preparing download...",
            progressPercent = 0,
            speedStr = "0 KB/s",
            etaStr = "--:--",
            taskId = "service_init"
        ).build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun observeActiveTasks() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            UniDownloaderApp.downloadManager.activeTasks.collectLatest { tasks ->
                val activeTask = tasks.firstOrNull { it.isActive }
                if (activeTask != null) {
                    val notification = NotificationManager.buildProgressNotification(
                        context = this@DownloadService,
                        title = activeTask.filename,
                        progressPercent = activeTask.progress.toInt(),
                        speedStr = com.unidownloader.app.utils.FormatUtils.formatSpeed(activeTask.speed),
                        etaStr = com.unidownloader.app.utils.FormatUtils.formatEta(activeTask.eta),
                        taskId = activeTask.id
                    ).build()

                    val androidNotificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    androidNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observerJob?.cancel()
    }
}
