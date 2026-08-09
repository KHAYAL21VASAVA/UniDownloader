package com.unidownloader.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.unidownloader.app.MainActivity
import com.unidownloader.app.R
import java.io.File

object NotificationManager {

    const val CHANNEL_DOWNLOAD_PROGRESS = "unidownloader_progress"
    const val CHANNEL_DOWNLOAD_COMPLETE = "unidownloader_complete"
    private const val NOTIFICATION_ID_BASE = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_PROGRESS,
                "Active Downloads",
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress, speed, and ETA"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_COMPLETE,
                "Download Completed",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when downloads finish successfully or fail"
                setShowBadge(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            manager.createNotificationChannels(listOf(progressChannel, completeChannel))
        }
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPercent: Int,
        speedStr: String,
        etaStr: String,
        taskId: String
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading: $title")
            .setContentText("$progressPercent% • $speedStr • ETA $etaStr")
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    fun showCompletedNotification(
        context: Context,
        title: String,
        savedFile: File,
        uri: Uri? = null
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            val fileUri = uri ?: Uri.fromFile(savedFile)
            val extension = savedFile.extension.lowercase()
            val mime = if (extension == "mp3" || extension == "m4a" || extension == "opus") "audio/*" else "video/*"
            setDataAndType(fileUri, mime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            savedFile.hashCode(),
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(savedFile.name)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID_BASE + (savedFile.name.hashCode() % 1000), notification)
    }

    fun cancelNotification(context: Context, taskId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        manager.cancel(NOTIFICATION_ID_BASE + (taskId.hashCode() % 1000))
    }
}
