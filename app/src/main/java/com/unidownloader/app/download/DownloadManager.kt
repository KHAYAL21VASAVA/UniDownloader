package com.unidownloader.app.download

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.unidownloader.app.data.model.DownloadHistoryItem
import com.unidownloader.app.data.model.MediaInfo
import com.unidownloader.app.data.repository.DownloadRepository
import com.unidownloader.app.data.repository.SettingsRepository
import com.unidownloader.app.ffmpeg.FFmpegManager
import com.unidownloader.app.notification.NotificationManager
import com.unidownloader.app.python.NativeStreamExtractor
import com.unidownloader.app.storage.DownloadStorage
import com.unidownloader.app.utils.FileNameUtils
import com.unidownloader.app.utils.FormatUtils
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val repository: DownloadRepository,
    private val storage: DownloadStorage,
    private val ffmpegManager: FFmpegManager,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "DownloadManager"
        private val SPEED_REGEX = Regex("""at\s+([0-9.]+\s*[KMGTP]?i?B/s)""", RegexOption.IGNORE_CASE)
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val nativeExtractor = NativeStreamExtractor(context)

    private val _activeTasks = MutableStateFlow<List<DownloadState>>(emptyList())
    val activeTasks: StateFlow<List<DownloadState>> = _activeTasks.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun startDownload(
        mediaInfo: MediaInfo,
        selectedVideoQuality: String,
        selectedAudioQuality: String,
        selectedFormat: String
    ): DownloadState {
        val extension = selectedFormat.lowercase()
        val isAudioOnly = selectedVideoQuality == "Audio only" || extension == "mp3" || extension == "m4a" || extension == "opus"
        val taskId = UUID.randomUUID().toString()
        val initialFileName = FileNameUtils.buildFileName(mediaInfo.title, extension)

        val task = DownloadState(
            id = taskId,
            url = mediaInfo.url,
            filename = initialFileName,
            status = "QUEUED",
            progress = 0f,
            stage = "Queued"
        )

        _activeTasks.update { listOf(task) + it.filter { t -> t.id != taskId } }
        startServiceIfRequired()

        val job = scope.launch {
            executeDownloadPipeline(task, mediaInfo, isAudioOnly, selectedVideoQuality, selectedAudioQuality, extension)
        }
        activeJobs[taskId] = job

        return task
    }

    fun cancelDownload(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()

        try {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        } catch (e: Exception) {
            Log.d(TAG, "Process destroy note: ${e.message}")
        }

        updateTask(taskId) {
            it.copy(
                status = "CANCELLED",
                stage = "Download cancelled"
            )
        }
        NotificationManager.cancelNotification(context, taskId)
    }

    private fun updateTask(id: String, updater: (DownloadState) -> DownloadState) {
        _activeTasks.update { list ->
            list.map { if (it.id == id) updater(it) else it }
        }
    }

    private fun startServiceIfRequired() {
        try {
            val serviceIntent = Intent(context, DownloadService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service start note: ${e.message}")
        }
    }

    private suspend fun executeDownloadPipeline(
        initialTask: DownloadState,
        mediaInfo: MediaInfo,
        isAudioOnly: Boolean,
        videoQuality: String,
        audioQuality: String,
        targetExtension: String
    ) {
        val taskId = initialTask.id
        val tempDir = storage.getTempCacheDir()
        val baseFileName = "download_${taskId}"
        val tempOutputFile = File(tempDir, "${baseFileName}.${targetExtension}")

        var downloadSuccess = false
        var producedFile: File? = null

        try {
            updateTask(taskId) {
                it.copy(
                    status = "DOWNLOADING",
                    stage = if (isAudioOnly) "Downloading audio stream..." else "Downloading video stream..."
                )
            }

            val isShortFormPlatform = mediaInfo.sourcePlatform == "Instagram" || mediaInfo.sourcePlatform == "TikTok"
            val hasDirectDownloadUrls = mediaInfo.formats.any { !it.downloadUrl.isNullOrBlank() }

            // Stage 1: For Instagram/TikTok or direct format URLs, download direct CDN streams immediately
            if (isShortFormPlatform || hasDirectDownloadUrls) {
                Log.i(TAG, "Executing high-speed direct stream download for ${mediaInfo.sourcePlatform}")
                producedFile = executeDirectStreamDownload(taskId, mediaInfo, isAudioOnly, targetExtension, tempDir)
                if (producedFile != null && isValidMediaFile(producedFile)) {
                    downloadSuccess = true
                } else {
                    producedFile?.delete()
                    producedFile = null
                }
            }

            // Stage 2: YoutubeDL Engine (for YouTube / other platforms)
            if (!downloadSuccess) {
                try {
                    Log.i(TAG, "Attempting download with YoutubeDL engine for: ${mediaInfo.url}")
                    val request = YoutubeDLRequest(mediaInfo.url)
                    val outTemplate = "${tempDir.absolutePath}/${baseFileName}.%(ext)s"
                    request.addOption("-o", outTemplate)
                    request.addOption("--no-playlist")
                    request.addOption("--no-check-certificates")
                    request.addOption("--force-overwrites")
                    request.addOption("--geo-bypass")
                    request.addOption("--no-mtime")
                    request.addOption("--extractor-args", "youtube:player_client=android,web;player_skip=configs")
                    request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    request.addOption("--retries", "5")

                    if (isAudioOnly) {
                        request.addOption("-f", "ba/bestaudio/best")
                        request.addOption("-x")
                        request.addOption("--audio-format", targetExtension)
                        when (audioQuality) {
                            "320 kbps" -> request.addOption("--audio-quality", "0")
                            "256 kbps" -> request.addOption("--audio-quality", "1")
                            "192 kbps" -> request.addOption("--audio-quality", "2")
                            else -> request.addOption("--audio-quality", "0")
                        }
                    } else {
                        val formatSelector = when (videoQuality) {
                            "2160p (4K)" -> "(bestvideo[height<=2160]+bestaudio/best[height<=2160]/bestvideo+bestaudio/18/22/best)"
                            "1440p (2K)" -> "(bestvideo[height<=1440]+bestaudio/best[height<=1440]/bestvideo+bestaudio/18/22/best)"
                            "1080p (Full HD)" -> "(bestvideo[height<=1080]+bestaudio/best[height<=1080]/bestvideo+bestaudio/18/22/best)"
                            "720p (HD)" -> "(bestvideo[height<=720]+bestaudio/best[height<=720]/bestvideo+bestaudio/18/22/best)"
                            "480p" -> "(bestvideo[height<=480]+bestaudio/best[height<=480]/bestvideo+bestaudio/18/22/best)"
                            "360p" -> "(bestvideo[height<=360]+bestaudio/best[height<=360]/bestvideo+bestaudio/18/22/best)"
                            else -> "(bestvideo+bestaudio/18/22/best)"
                        }
                        request.addOption("-f", formatSelector)
                        request.addOption("-S", "res,ext:mp4:m4a")
                        request.addOption("--merge-output-format", targetExtension)
                    }

                    YoutubeDL.getInstance().execute(request, taskId) { progress, etaInSeconds, line ->
                        var stageText = "Downloading..."
                        var speedBytes = 0L

                        if (!line.isNullOrBlank()) {
                            if (line.contains("Merging", ignoreCase = true) || line.contains("ffmpeg", ignoreCase = true)) {
                                stageText = "Merging video & audio..."
                            } else if (line.contains("ExtractAudio", ignoreCase = true) || line.contains("Destination", ignoreCase = true)) {
                                stageText = "Extracting audio..."
                            } else if (line.contains("Downloading video", ignoreCase = true)) {
                                stageText = "Downloading video stream..."
                            } else if (line.contains("Downloading audio", ignoreCase = true)) {
                                stageText = "Downloading audio stream..."
                            }

                            SPEED_REGEX.find(line)?.let { match ->
                                speedBytes = parseSpeedToBytes(match.groupValues[1])
                            }
                        }

                        updateTask(taskId) { current ->
                            current.copy(
                                progress = progress.coerceIn(0f, 98f),
                                speed = if (speedBytes > 0) speedBytes else current.speed,
                                eta = etaInSeconds,
                                stage = stageText
                            )
                        }
                    }

                    if (tempOutputFile.exists() && isValidMediaFile(tempOutputFile)) {
                        producedFile = tempOutputFile
                        downloadSuccess = true
                    } else {
                        val matching = tempDir.listFiles { _, name ->
                            name.startsWith(baseFileName) &&
                            !name.endsWith(".part") &&
                            !name.endsWith(".tmp") &&
                            !name.endsWith(".ytdl")
                        }
                        val validFiles = matching?.filter { isValidMediaFile(it) }
                        val largest = validFiles?.maxByOrNull { it.length() }
                        if (largest != null) {
                            producedFile = largest
                            downloadSuccess = true
                        }
                    }
                } catch (ytdlError: Throwable) {
                    Log.e(TAG, "YoutubeDL engine error: ${ytdlError.message}")
                }
            }

            // Stage 3: Fallback Cloud & Stream Scraper
            if (!downloadSuccess || producedFile == null) {
                Log.i(TAG, "Executing cloud fallback direct stream download for $taskId")
                producedFile = executeDirectStreamDownload(taskId, mediaInfo, isAudioOnly, targetExtension, tempDir)
                downloadSuccess = producedFile != null && isValidMediaFile(producedFile)
            }

            if (!downloadSuccess || producedFile == null || !producedFile.exists() || producedFile.length() == 0L) {
                throw IllegalStateException("Download was unable to complete. Please try another quality or check the link.")
            }

            // Stage 4: Save to Public Downloads (MediaStore)
            updateTask(taskId) {
                it.copy(
                    status = "PROCESSING",
                    stage = "Saving to Downloads folder...",
                    progress = 99f
                )
            }

            val (savedFile, contentUri) = storage.saveToPublicDownloads(
                sourceTempFile = producedFile,
                desiredTitle = mediaInfo.title,
                extension = targetExtension,
                sourceUrl = mediaInfo.url
            )

            producedFile.delete()
            tempOutputFile.delete()

            // Stage 5: Add to History
            val historyItem = DownloadHistoryItem(
                id = taskId,
                title = mediaInfo.title,
                fileName = savedFile.name,
                extension = targetExtension.uppercase(),
                fileSizeBytes = savedFile.length(),
                filePath = savedFile.absolutePath,
                fileUriString = contentUri?.toString() ?: savedFile.absolutePath,
                mimeType = FormatUtils.getMimeTypeFromExtension(targetExtension),
                thumbnailUrl = mediaInfo.thumbnail,
                durationSeconds = mediaInfo.durationSeconds,
                downloadedAt = System.currentTimeMillis(),
                sourceUrl = mediaInfo.url
            )
            repository.addHistoryItem(historyItem)

            updateTask(taskId) {
                it.copy(
                    status = "COMPLETED",
                    progress = 100f,
                    downloadedBytes = savedFile.length(),
                    totalBytes = savedFile.length(),
                    stage = "Download complete",
                    outputPath = savedFile.absolutePath
                )
            }

            if (settingsRepository.settings.value.downloadNotificationsEnabled) {
                NotificationManager.showCompletedNotification(
                    context = context,
                    title = mediaInfo.title,
                    savedFile = savedFile,
                    uri = contentUri
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download error for task $taskId: ${e.message}", e)
            updateTask(taskId) {
                it.copy(
                    status = "FAILED",
                    error = e.message ?: "Download failed",
                    stage = "Download failed"
                )
            }
        } finally {
            activeJobs.remove(taskId)
        }
    }

    private suspend fun executeDirectStreamDownload(
        taskId: String,
        mediaInfo: MediaInfo,
        isAudioOnly: Boolean,
        targetExtension: String,
        tempDir: File
    ): File? = withContext(Dispatchers.IO) {
        var currentInfo = mediaInfo

        // If current mediaInfo doesn't have direct download URLs, re-extract with native multi-tier scraper
        if (currentInfo.formats.none { !it.downloadUrl.isNullOrBlank() }) {
            try {
                currentInfo = nativeExtractor.extract(mediaInfo.url)
            } catch (e: Exception) {
                Log.d(TAG, "Re-extraction note: ${e.message}")
            }
        }

        val matchingVideoFormat = currentInfo.formats.firstOrNull {
            it.isVideo && !it.downloadUrl.isNullOrBlank()
        }
        val matchingAudioFormat = currentInfo.formats.firstOrNull {
            it.isAudioOnly && !it.downloadUrl.isNullOrBlank()
        }

        if (isAudioOnly) {
            val audioUrl = matchingAudioFormat?.downloadUrl ?: matchingVideoFormat?.downloadUrl ?: mediaInfo.url
            val targetTempFile = File(tempDir, "direct_audio_${taskId}.$targetExtension")

            // If audio source is embedded in a video file (e.g. Instagram/TikTok), extract audio with FFmpeg
            if (audioUrl.contains(".mp4", ignoreCase = true) || audioUrl.contains("cdninstagram.com", ignoreCase = true) || audioUrl.contains("tiktok", ignoreCase = true)) {
                val tempVideo = File(tempDir, "source_vid_${taskId}.mp4")
                val downloaded = downloadHttpStreamWithFullHeaders(taskId, audioUrl, mediaInfo.url, tempVideo)
                if (downloaded && isValidMediaFile(tempVideo)) {
                    val extracted = ffmpegManager.extractAudio(tempVideo, targetTempFile, targetExtension)
                    tempVideo.delete()
                    if (extracted && isValidMediaFile(targetTempFile)) {
                        return@withContext targetTempFile
                    }
                }
            } else {
                val downloaded = downloadHttpStreamWithFullHeaders(taskId, audioUrl, mediaInfo.url, targetTempFile)
                if (downloaded && isValidMediaFile(targetTempFile)) {
                    return@withContext targetTempFile
                }
            }
        }

        // Direct muxed video download
        if (matchingVideoFormat?.isMuxed == true || matchingAudioFormat == null) {
            val videoUrl = matchingVideoFormat?.downloadUrl ?: mediaInfo.url
            val targetTempFile = File(tempDir, "direct_video_${taskId}.$targetExtension")
            val downloaded = downloadHttpStreamWithFullHeaders(taskId, videoUrl, mediaInfo.url, targetTempFile)
            if (downloaded && isValidMediaFile(targetTempFile)) {
                return@withContext targetTempFile
            }
        }

        // Separate video and audio streams merge with FFmpeg
        val tempVideo = File(tempDir, "video_part_${taskId}.mp4")
        val tempAudio = File(tempDir, "audio_part_${taskId}.m4a")
        val targetMerged = File(tempDir, "direct_merged_${taskId}.$targetExtension")

        val videoUrl = matchingVideoFormat?.downloadUrl
        val audioUrl = matchingAudioFormat?.downloadUrl

        if (!videoUrl.isNullOrBlank() && !audioUrl.isNullOrBlank()) {
            val videoOk = downloadHttpStreamWithFullHeaders(taskId, videoUrl, mediaInfo.url, tempVideo)
            val audioOk = downloadHttpStreamWithFullHeaders(taskId, audioUrl, mediaInfo.url, tempAudio)

            if (videoOk && audioOk && isValidMediaFile(tempVideo) && tempAudio.exists()) {
                val merged = ffmpegManager.mergeVideoAndAudio(tempVideo, tempAudio, targetMerged)
                tempVideo.delete()
                tempAudio.delete()
                if (merged && isValidMediaFile(targetMerged)) {
                    return@withContext targetMerged
                }
            }
        }

        if (tempVideo.exists() && isValidMediaFile(tempVideo)) tempVideo else null
    }

    private fun downloadHttpStreamWithFullHeaders(
        taskId: String,
        streamUrl: String,
        refererUrl: String,
        targetFile: File
    ): Boolean {
        val request = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Sec-Fetch-Dest", "video")
            .header("Sec-Fetch-Mode", "no-cors")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Referer", if (refererUrl.isNotBlank()) refererUrl else "https://www.instagram.com/")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Direct download failed with HTTP ${response.code}: ${response.message}")
                    return false
                }

                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("text/html", ignoreCase = true) || contentType.contains("text/plain", ignoreCase = true)) {
                    Log.w(TAG, "Direct download rejected HTML stream payload: $contentType")
                    return false
                }

                val body = response.body ?: return false
                val totalBytes = body.contentLength().coerceAtLeast(1L)
                var downloadedBytes = 0L

                var lastTime = System.currentTimeMillis()
                var bytesSinceLastTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            bytesSinceLastTime += bytesRead

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastTime
                            if (elapsed >= 500) {
                                val currentSpeed = (bytesSinceLastTime * 1000) / elapsed
                                val remainingBytes = (totalBytes - downloadedBytes).coerceAtLeast(0L)
                                val eta = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L
                                val percent = ((downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 98f)

                                updateTask(taskId) {
                                    it.copy(
                                        progress = percent,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes,
                                        speed = currentSpeed,
                                        eta = eta,
                                        stage = "Downloading media stream..."
                                    )
                                }
                                lastTime = now
                                bytesSinceLastTime = 0L
                            }
                        }
                        output.flush()
                    }
                }

                if (isValidMediaFile(targetFile)) {
                    true
                } else {
                    targetFile.delete()
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP stream error: ${e.message}")
            targetFile.delete()
            false
        }
    }

    private fun isValidMediaFile(file: File): Boolean {
        if (!file.exists() || file.length() < 50_000L) return false
        return try {
            val header = ByteArray(256)
            val read = file.inputStream().use { it.read(header) }
            if (read <= 0) return false
            val headerStr = String(header, 0, read, Charsets.UTF_8).lowercase()
            // Reject HTML web pages (e.g. 589.7 KB login redirect)
            if (headerStr.contains("<!doctype") || headerStr.contains("<html") ||
                headerStr.contains("<head") || headerStr.contains("window._shareddata") ||
                headerStr.contains("login • instagram")) {
                Log.w(TAG, "File rejected: detected HTML content in media file: ${file.name}")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseSpeedToBytes(speedStr: String): Long {
        return try {
            val clean = speedStr.trim()
            val num = clean.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            when {
                clean.contains("GiB", ignoreCase = true) || clean.contains("GB", ignoreCase = true) -> (num * 1024 * 1024 * 1024).toLong()
                clean.contains("MiB", ignoreCase = true) || clean.contains("MB", ignoreCase = true) -> (num * 1024 * 1024).toLong()
                clean.contains("KiB", ignoreCase = true) || clean.contains("KB", ignoreCase = true) -> (num * 1024).toLong()
                else -> num.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
}
