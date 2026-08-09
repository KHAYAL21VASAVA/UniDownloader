package com.unidownloader.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.unidownloader.app.MainActivity
import com.unidownloader.app.R
import com.unidownloader.app.data.model.DownloadTask
import com.unidownloader.app.utils.FormatUtils
import java.io.File

object NotificationHelper {

    const val CHANNEL_DOWNLOADS = "unidownloader_active_downloads"
    const val CHANNEL_COMPLETED = "unidownloader_completed_downloads"

    private const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val activeChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress and speed"
                setShowBadge(false)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Download Completed",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when a media download finishes"
                setShowBadge(true)
            }

            manager.createNotificationChannel(activeChannel)
            manager.createNotificationChannel(completedChannel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        task: DownloadTask
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = task.progressPercent.toInt().coerceIn(0, 100)
        val speedStr = FormatUtils.formatSpeed(task.speedBytesPerSec)
        val downloadedStr = FormatUtils.formatBytes(task.downloadedBytes)
        val totalStr = if (task.totalBytes > 0) FormatUtils.formatBytes(task.totalBytes) else "--"

        val contentText = "$percent% • $speedStr • $downloadedStr / $totalStr"

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(task.mediaTitle.ifBlank { "Downloading Media" })
            .setContentText(contentText)
            .setSubText(task.stageDescription)
            .setProgress(100, percent, task.progressPercent <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showCompletedNotification(
        context: Context,
        title: String,
        savedFile: File,
        uri: Uri? = null
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            val contentUri = uri ?: try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    savedFile
                )
            } catch (e: Exception) {
                Uri.fromFile(savedFile)
            }
            val mime = FormatUtils.getMimeTypeFromExtension(savedFile.extension)
            setDataAndType(contentUri, mime)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            savedFile.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(savedFile.name)
            .setSubText("Saved to Downloads")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID_BASE + savedFile.name.hashCode() % 1000, notification)
    }
}
