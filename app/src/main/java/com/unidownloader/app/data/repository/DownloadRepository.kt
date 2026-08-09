package com.unidownloader.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unidownloader.app.data.model.DownloadHistoryItem
import com.unidownloader.app.data.model.DownloadStatus
import com.unidownloader.app.data.model.DownloadTask
import com.unidownloader.app.storage.DownloadStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class DownloadRepository(
    private val context: Context,
    private val storage: DownloadStorage
) {
    companion object {
        private const val TAG = "DownloadRepository"
        private const val PREF_HISTORY_KEY = "download_history_json"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("unidownloader_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _activeTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val activeTasks: StateFlow<List<DownloadTask>> = _activeTasks.asStateFlow()

    private val _history = MutableStateFlow<List<DownloadHistoryItem>>(loadHistory())
    val history: StateFlow<List<DownloadHistoryItem>> = _history.asStateFlow()

    init {
        // Scan public Downloads folder to synchronize existing media
        scope.launch {
            syncHistoryWithStorage()
        }
    }

    fun addTask(task: DownloadTask) {
        _activeTasks.update { current ->
            listOf(task) + current.filter { it.id != task.id }
        }
    }

    fun updateTask(id: String, updater: (DownloadTask) -> DownloadTask) {
        _activeTasks.update { list ->
            list.map { if (it.id == id) updater(it) else it }
        }
    }

    fun removeTask(id: String) {
        _activeTasks.update { list -> list.filter { it.id != id } }
    }

    fun addHistoryItem(item: DownloadHistoryItem) {
        _history.update { current ->
            val updated = listOf(item) + current.filter { it.id != item.id && it.filePath != item.filePath }
            saveHistory(updated)
            updated
        }
    }

    fun deleteHistoryItem(item: DownloadHistoryItem) {
        scope.launch {
            storage.deleteDownloadedFile(item)
            _history.update { current ->
                val updated = current.filter { it.id != item.id }
                saveHistory(updated)
                updated
            }
        }
    }

    fun clearAllHistory() {
        _history.value = emptyList()
        prefs.edit().remove(PREF_HISTORY_KEY).apply()
    }

    private fun loadHistory(): List<DownloadHistoryItem> {
        val json = prefs.getString(PREF_HISTORY_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadHistoryItem>>() {}.type
            val items: List<DownloadHistoryItem> = gson.fromJson(json, type) ?: emptyList()
            // Filter out any hidden or system trashed files
            items.filter { !it.fileName.startsWith(".") && !it.fileName.contains(".trashed", ignoreCase = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<DownloadHistoryItem>) {
        try {
            val filtered = list.filter { !it.fileName.startsWith(".") && !it.fileName.contains(".trashed", ignoreCase = true) }
            val json = gson.toJson(filtered)
            prefs.edit().putString(PREF_HISTORY_KEY, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history JSON: ${e.message}")
        }
    }

    private fun syncHistoryWithStorage() {
        try {
            val downloadsDir = storage.getPublicDownloadsDirectory()
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                val files = downloadsDir.listFiles() ?: return
                val currentHistory = _history.value.filter { !it.fileName.startsWith(".") && !it.fileName.contains(".trashed", ignoreCase = true) }.toMutableList()

                for (file in files) {
                    val name = file.name
                    if (file.isFile && !name.startsWith(".") && !name.contains(".trashed", ignoreCase = true) &&
                        (file.extension.equals("mp4", true) || file.extension.equals("mp3", true) || file.extension.equals("mkv", true) || file.extension.equals("m4a", true) || file.extension.equals("webm", true) || file.extension.equals("opus", true))) {
                        val alreadyTracked = currentHistory.any { it.filePath == file.absolutePath || it.fileName == file.name }
                        if (!alreadyTracked) {
                            val newItem = DownloadHistoryItem(
                                title = file.nameWithoutExtension,
                                fileName = file.name,
                                extension = file.extension.uppercase(),
                                fileSizeBytes = file.length(),
                                filePath = file.absolutePath,
                                fileUriString = file.absolutePath,
                                mimeType = com.unidownloader.app.utils.FormatUtils.getMimeTypeFromExtension(file.extension),
                                downloadedAt = file.lastModified()
                            )
                            currentHistory.add(newItem)
                        }
                    }
                }
                val sorted = currentHistory.sortedByDescending { it.downloadedAt }
                _history.value = sorted
                saveHistory(sorted)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Storage sync note: ${e.message}")
        }
    }
}
