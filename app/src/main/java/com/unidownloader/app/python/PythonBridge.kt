package com.unidownloader.app.python

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.unidownloader.app.data.model.MediaFormat
import com.unidownloader.app.data.model.MediaInfo
import com.unidownloader.app.utils.UrlUtils
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PythonBridge(private val context: Context) {

    companion object {
        private const val TAG = "PythonBridge"
    }

    private val nativeExtractor = NativeStreamExtractor(context)
    private val gson = Gson()

    suspend fun analyze(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val cleanUrl = UrlUtils.cleanUrl(url)
        val platform = UrlUtils.detectPlatform(cleanUrl)

        // 1. For Instagram and TikTok, use dedicated native scrapers first (avoids unauthenticated login HTML pages)
        if (platform == "Instagram" || platform == "TikTok") {
            try {
                val nativeInfo = nativeExtractor.extract(cleanUrl)
                if (nativeInfo.formats.isNotEmpty()) {
                    Log.i(TAG, "Direct scraper success for $platform: ${nativeInfo.title}")
                    return@withContext nativeInfo
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Direct scraper pass note: ${e.message}")
            }
        }

        // 2. Primary High-Performance Engine (yt-dlp Python engine with Android spoofing)
        try {
            val request = YoutubeDLRequest(cleanUrl)
            request.addOption("--dump-single-json")
            request.addOption("--no-playlist")
            request.addOption("--no-check-certificates")
            request.addOption("--geo-bypass")
            request.addOption("--extractor-args", "youtube:player_client=android,web;player_skip=configs")
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")

            val response = YoutubeDL.getInstance().execute(request)
            val jsonString = response.out
            if (!jsonString.isNullOrBlank() && jsonString.startsWith("{")) {
                val mediaInfo = parseMediaJson(jsonString, cleanUrl, platform)
                if (mediaInfo != null && mediaInfo.formats.isNotEmpty()) {
                    Log.i(TAG, "Analysis successful via Python engine: ${mediaInfo.title}")
                    return@withContext mediaInfo
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Python engine pass note: ${e.message}")
        }

        // 3. Resilient Multi-Tier Scraper Fallback (Cobalt / OpenGraph / Direct)
        return@withContext nativeExtractor.extract(cleanUrl)
    }

    private fun parseMediaJson(jsonString: String, originalUrl: String, platform: String): MediaInfo? {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            var title = root.optString("title", "Media Download")
            val uploader = root.optString("uploader", root.optString("channel", platform))
            val thumbnail = root.optString("thumbnail", null)
            val duration = root.optLong("duration", 0L)
            val description = root.optString("description", null)

            // Clean Instagram titles
            if (platform == "Instagram" && (title.equals("Instagram", true) || title.startsWith("Video by", true))) {
                val code = Regex("""/(?:reel|p|reels)/([A-Za-z0-9_-]+)""").find(originalUrl)?.groupValues?.getOrNull(1)
                title = if (code != null) "Instagram_Reel_$code" else "Instagram_Media"
            }

            val formatList = mutableListOf<MediaFormat>()
            val formatsArray = root.getAsJsonArray("formats")
            if (formatsArray != null) {
                for (item in formatsArray) {
                    if (!item.isJsonObject) continue
                    val fObj = item.asJsonObject
                    val formatId = fObj.optString("format_id", "")
                    val ext = fObj.optString("ext", "mp4")
                    val width = fObj.optInt("width", 0)
                    val height = fObj.optInt("height", 0)
                    val fps = fObj.optInt("fps", 0)
                    val vcodec = fObj.optString("vcodec", "none")
                    val acodec = fObj.optString("acodec", "none")
                    val abr = fObj.optDouble("abr", 0.0)
                    val tbr = fObj.optDouble("tbr", 0.0)
                    val filesize = fObj.optLong("filesize", 0L)
                    val downloadUrl = fObj.optString("url", null)

                    val isVideo = vcodec != "none" && vcodec.isNotBlank()
                    val isAudio = acodec != "none" && acodec.isNotBlank()

                    if (isVideo || isAudio) {
                        formatList.add(
                            MediaFormat(
                                formatId = formatId,
                                ext = ext,
                                resolution = if (height > 0) "${height}p" else if (isVideo) "Video" else "Audio only",
                                height = height,
                                width = width,
                                fps = fps,
                                vcodec = if (isVideo) vcodec else null,
                                acodec = if (isAudio) acodec else null,
                                abr = abr,
                                tbr = tbr,
                                filesize = filesize,
                                downloadUrl = downloadUrl,
                                isVideo = isVideo,
                                isAudioOnly = !isVideo && isAudio,
                                isMuxed = isVideo && isAudio
                            )
                        )
                    }
                }
            }

            val sorted = formatList.sortedWith(
                compareByDescending<MediaFormat> { it.height }
                    .thenByDescending { it.tbr }
                    .thenByDescending { it.abr }
            )

            MediaInfo(
                url = originalUrl,
                title = title,
                uploader = uploader,
                thumbnail = thumbnail,
                durationSeconds = duration,
                sourcePlatform = platform,
                formats = if (sorted.isNotEmpty()) sorted else emptyList(),
                description = description,
                extractorName = "PythonBridge"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Python JSON: ${e.message}")
            null
        }
    }

    private fun JsonObject.optString(key: String, fallback: String? = null): String {
        return if (has(key) && !get(key).isJsonNull) get(key).asString else (fallback ?: "")
    }

    private fun JsonObject.optLong(key: String, fallback: Long = 0L): Long {
        return if (has(key) && !get(key).isJsonNull) {
            try { get(key).asLong } catch (e: Exception) { fallback }
        } else fallback
    }

    private fun JsonObject.optInt(key: String, fallback: Int = 0): Int {
        return if (has(key) && !get(key).isJsonNull) {
            try { get(key).asInt } catch (e: Exception) { fallback }
        } else fallback
    }

    private fun JsonObject.optDouble(key: String, fallback: Double = 0.0): Double {
        return if (has(key) && !get(key).isJsonNull) {
            try { get(key).asDouble } catch (e: Exception) { fallback }
        } else fallback
    }
}
