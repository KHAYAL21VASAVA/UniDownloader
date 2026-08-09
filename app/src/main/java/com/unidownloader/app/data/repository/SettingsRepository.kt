package com.unidownloader.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.unidownloader.app.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("unidownloader_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            defaultVideoQuality = prefs.getString("default_video_quality", "Best") ?: "Best",
            defaultAudioQuality = prefs.getString("default_audio_quality", "Best") ?: "Best",
            defaultFormat = prefs.getString("default_format", "MP4") ?: "MP4",
            downloadNotificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            wifiOnlyDownloads = prefs.getBoolean("wifi_only", false),
            maxConcurrentDownloads = prefs.getInt("max_concurrent", 3),
            autoClearCompleted = prefs.getBoolean("auto_clear", false)
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit()
            .putString("default_video_quality", newSettings.defaultVideoQuality)
            .putString("default_audio_quality", newSettings.defaultAudioQuality)
            .putString("default_format", newSettings.defaultFormat)
            .putBoolean("notifications_enabled", newSettings.downloadNotificationsEnabled)
            .putBoolean("wifi_only", newSettings.wifiOnlyDownloads)
            .putInt("max_concurrent", newSettings.maxConcurrentDownloads)
            .putBoolean("auto_clear", newSettings.autoClearCompleted)
            .apply()
        _settings.value = newSettings
    }

    fun updateDefaultVideoQuality(quality: String) {
        updateSettings(_settings.value.copy(defaultVideoQuality = quality))
    }

    fun updateDefaultAudioFormat(format: String) {
        updateSettings(_settings.value.copy(defaultFormat = format))
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        updateSettings(_settings.value.copy(wifiOnlyDownloads = wifiOnly))
    }

    fun updateNotifications(enabled: Boolean) {
        updateSettings(_settings.value.copy(downloadNotificationsEnabled = enabled))
    }
}
