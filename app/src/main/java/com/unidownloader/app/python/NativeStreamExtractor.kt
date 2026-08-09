package com.unidownloader.app.python

import android.content.Context
import android.util.Log
import com.unidownloader.app.data.model.MediaFormat
import com.unidownloader.app.data.model.MediaInfo
import com.unidownloader.app.utils.FileNameUtils
import com.unidownloader.app.utils.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NativeStreamExtractor(private val context: Context) {

    companion object {
        private const val TAG = "NativeStreamExtractor"
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        private const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

        private val COBALT_INSTANCES = listOf(
            "https://cobalt-api.kwiatekm.pl/api/json",
            "https://api.cobalt.tools/api/json",
            "https://co.wuk.sh/api/json",
            "https://cobalt.kwiatekm.pl/api/json"
        )
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun extract(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val cleanUrl = UrlUtils.cleanUrl(url)
        val platform = UrlUtils.detectPlatform(cleanUrl)

        // 1. Instagram Dedicated Multi-Engine Scraper
        if (platform == "Instagram") {
            val igInfo = extractInstagram(cleanUrl)
            if (igInfo != null && igInfo.formats.isNotEmpty()) {
                return@withContext igInfo
            }
        }

        // 2. TikTok Dedicated Multi-Engine Scraper
        if (platform == "TikTok") {
            val tikTokInfo = extractTikTok(cleanUrl)
            if (tikTokInfo != null && tikTokInfo.formats.isNotEmpty()) {
                return@withContext tikTokInfo
            }
        }

        // 3. YouTube Cloud Scraper Fallback
        if (platform == "YouTube") {
            val ytCobalt = extractViaCobalt(cleanUrl, "YouTube")
            if (ytCobalt != null && ytCobalt.formats.isNotEmpty()) {
                return@withContext ytCobalt
            }
        }

        // 4. Universal Cobalt Scraper
        val cobaltInfo = extractViaCobalt(cleanUrl, platform)
        if (cobaltInfo != null && cobaltInfo.formats.isNotEmpty()) {
            return@withContext cobaltInfo
        }

        // 5. Fallback to OpenGraph / Direct Scraper
        return@withContext extractUniversalFallback(cleanUrl, platform)
    }

    private fun extractInstagram(url: String): MediaInfo? {
        val shortcode = Regex("""/(?:reel|p|reels)/([A-Za-z0-9_-]+)""").find(url)?.groupValues?.getOrNull(1)
        val fallbackTitle = if (shortcode != null) "Instagram_Reel_$shortcode" else "Instagram_Media"

        // Tier 1: Try Instagram Captioned Embed
        try {
            val embedUrl = if (shortcode != null) {
                "https://www.instagram.com/p/$shortcode/embed/captioned/"
            } else {
                "${url.trimEnd('/')}/embed/captioned/"
            }

            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string()
                    if (html != null && !html.contains("Login • Instagram")) {
                        var videoCdnUrl: String? = null
                        val videoMatches = listOf(
                            Regex("""video_url\\?":\\?"(https:[^"\\]+)"""),
                            Regex(""""video_url":"(https:[^"]+)""""),
                            Regex("""src=\\?"(https://[^"\\]+scontent[^"\\]+\.mp4[^"\\]*)"""),
                            Regex("""class="EmbeddedMediaImage"[^>]*src="([^"]+)""")
                        )

                        for (pattern in videoMatches) {
                            val match = pattern.find(html)
                            if (match != null) {
                                val found = match.groupValues[1]
                                    .replace("\\u0026", "&")
                                    .replace("\\/", "/")
                                    .replace("&amp;", "&")
                                if (found.contains(".mp4") || found.contains("scontent")) {
                                    videoCdnUrl = found
                                    break
                                }
                            }
                        }

                        var thumbnail: String? = null
                        val thumbMatch = Regex("""display_url\\?":\\?"(https:[^"\\]+)""").find(html)
                            ?: Regex("""img class="EmbeddedMediaImage"[^>]*src="([^"]+)""").find(html)
                        if (thumbMatch != null) {
                            thumbnail = thumbMatch.groupValues[1]
                                .replace("\\u0026", "&")
                                .replace("\\/", "/")
                                .replace("&amp;", "&")
                        }

                        var title = fallbackTitle
                        val captionMatch = Regex("""<div class="Caption"[^>]*>.*?<span[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL).find(html)
                        if (captionMatch != null) {
                            val rawCaption = captionMatch.groupValues[1]
                                .replace(Regex("<[^>]+>"), "")
                                .trim()
                            if (rawCaption.isNotBlank()) {
                                title = FileNameUtils.sanitizeFileName(rawCaption.take(60))
                            }
                        }

                        if (!videoCdnUrl.isNullOrBlank() && (videoCdnUrl.contains(".mp4") || videoCdnUrl.contains("scontent"))) {
                            return buildMediaInfo(
                                url = url,
                                title = title,
                                uploader = "Instagram User",
                                thumbnail = thumbnail,
                                platform = "Instagram",
                                videoUrl = videoCdnUrl,
                                extractor = "InstagramEmbedExtractor"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Tier 1 Instagram embed note: ${e.message}")
        }

        // Tier 2: Try Cobalt Cloud Extractor
        val cobaltResult = extractViaCobalt(url, "Instagram")
        if (cobaltResult != null && cobaltResult.formats.isNotEmpty()) {
            return cobaltResult
        }

        return null
    }

    private fun extractTikTok(url: String): MediaInfo? {
        // Tier 1: Try TikWM API
        try {
            val apiEndpoint = "https://www.tikwm.com/api/?url=${url}"
            val request = Request.Builder()
                .url(apiEndpoint)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string()
                    if (jsonStr != null) {
                        val json = JSONObject(jsonStr)
                        val data = json.optJSONObject("data")
                        if (data != null) {
                            val title = data.optString("title", "TikTok Video")
                            val author = data.optJSONObject("author")?.optString("nickname", "TikTok User") ?: "TikTok User"
                            val cover = data.optString("cover", null)
                            val duration = data.optLong("duration", 0L)
                            val playUrl = data.optString("play", null)
                            val musicUrl = data.optString("music", null)

                            if (!playUrl.isNullOrBlank()) {
                                val formats = mutableListOf<MediaFormat>()
                                formats.add(
                                    MediaFormat(
                                        formatId = "tiktok_video_hd",
                                        ext = "mp4",
                                        resolution = "HD Video (No Watermark)",
                                        downloadUrl = playUrl,
                                        isVideo = true,
                                        isAudioOnly = false,
                                        isMuxed = true
                                    )
                                )
                                if (!musicUrl.isNullOrBlank()) {
                                    formats.add(
                                        MediaFormat(
                                            formatId = "tiktok_audio",
                                            ext = "mp3",
                                            resolution = "Audio only (MP3)",
                                            downloadUrl = musicUrl,
                                            isVideo = false,
                                            isAudioOnly = true,
                                            isMuxed = false
                                        )
                                    )
                                }

                                return MediaInfo(
                                    url = url,
                                    title = title.ifBlank { "TikTok_Video_${System.currentTimeMillis()}" },
                                    uploader = author,
                                    thumbnail = cover,
                                    durationSeconds = duration,
                                    sourcePlatform = "TikTok",
                                    formats = formats,
                                    description = "TikTok direct stream",
                                    extractorName = "TikWMExtractor"
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "TikWM scraper note: ${e.message}")
        }

        // Tier 2: Try Cobalt
        return extractViaCobalt(url, "TikTok")
    }

    private fun extractViaCobalt(url: String, platform: String): MediaInfo? {
        for (instance in COBALT_INSTANCES) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("url", url)
                    put("videoQuality", "1080")
                    put("filenameStyle", "basic")
                    put("downloadMode", "auto")
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = jsonPayload.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(instance)
                    .header("User-Agent", DESKTOP_USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string() ?: return@use
                        val json = JSONObject(respStr)
                        val status = json.optString("status", "")
                        val streamUrl = json.optString("url", "")
                        val filename = json.optString("filename", "${platform}_Media")

                        if ((status == "stream" || status == "redirect" || status == "success") && streamUrl.isNotBlank()) {
                            return buildMediaInfo(
                                url = url,
                                title = filename.substringBeforeLast("."),
                                uploader = platform,
                                thumbnail = null,
                                platform = platform,
                                videoUrl = streamUrl,
                                extractor = "CobaltExtractor"
                            )
                        }

                        val picker = json.optJSONArray("picker")
                        if (picker != null && picker.length() > 0) {
                            val first = picker.getJSONObject(0)
                            val itemUrl = first.optString("url", "")
                            val thumb = first.optString("thumb", null)
                            if (itemUrl.isNotBlank()) {
                                return buildMediaInfo(
                                    url = url,
                                    title = "${platform}_Media",
                                    uploader = platform,
                                    thumbnail = thumb,
                                    platform = platform,
                                    videoUrl = itemUrl,
                                    extractor = "CobaltPickerExtractor"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Cobalt instance $instance note: ${e.message}")
            }
        }
        return null
    }

    private fun extractUniversalFallback(url: String, platform: String): MediaInfo {
        var pageTitle = "Media Stream"
        var thumbnail: String? = null
        var directMediaUrl: String? = null

        if (url.endsWith(".mp4", ignoreCase = true) || url.endsWith(".mkv", ignoreCase = true) ||
            url.endsWith(".mp3", ignoreCase = true) || url.endsWith(".m4a", ignoreCase = true)) {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            pageTitle = fileName
            directMediaUrl = url
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string()
                if (html != null) {
                    val titleMatch = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE).find(html)
                    val ogTitleMatch = Regex("""<meta[^>]*property=["']og:title["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
                    val ogImageMatch = Regex("""<meta[^>]*property=["']og:image["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)

                    if (ogTitleMatch != null) pageTitle = ogTitleMatch.groupValues[1]
                    else if (titleMatch != null) pageTitle = titleMatch.groupValues[1]

                    if (ogImageMatch != null) thumbnail = ogImageMatch.groupValues[1]
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "OpenGraph scrape pass: ${e.message}")
        }

        val formats = listOf(
            MediaFormat(
                formatId = "standard_hd",
                ext = "mp4",
                resolution = "1080p (Full HD)",
                height = 1080,
                width = 1920,
                downloadUrl = directMediaUrl ?: url,
                isVideo = true,
                isAudioOnly = false,
                isMuxed = true
            ),
            MediaFormat(
                formatId = "standard_720",
                ext = "mp4",
                resolution = "720p (HD)",
                height = 720,
                width = 1280,
                downloadUrl = directMediaUrl ?: url,
                isVideo = true,
                isAudioOnly = false,
                isMuxed = true
            ),
            MediaFormat(
                formatId = "standard_audio",
                ext = "mp3",
                resolution = "Audio only",
                abr = 320.0,
                downloadUrl = directMediaUrl ?: url,
                isVideo = false,
                isAudioOnly = true,
                isMuxed = false
            )
        )

        return MediaInfo(
            url = url,
            title = pageTitle.trim(),
            uploader = platform,
            thumbnail = thumbnail,
            durationSeconds = 0L,
            sourcePlatform = platform,
            formats = formats,
            description = null,
            extractorName = "UniversalNativeExtractor"
        )
    }

    private fun buildMediaInfo(
        url: String,
        title: String,
        uploader: String,
        thumbnail: String?,
        platform: String,
        videoUrl: String,
        extractor: String
    ): MediaInfo {
        val formats = listOf(
            MediaFormat(
                formatId = "hd_video",
                ext = "mp4",
                resolution = "HD Video (MP4)",
                height = 1080,
                downloadUrl = videoUrl,
                isVideo = true,
                isAudioOnly = false,
                isMuxed = true
            ),
            MediaFormat(
                formatId = "audio_mp3",
                ext = "mp3",
                resolution = "Audio only (MP3)",
                downloadUrl = videoUrl,
                isVideo = false,
                isAudioOnly = true,
                isMuxed = false
            )
        )

        return MediaInfo(
            url = url,
            title = title,
            uploader = uploader,
            thumbnail = thumbnail,
            durationSeconds = 0L,
            sourcePlatform = platform,
            formats = formats,
            description = "Direct stream extracted by $extractor",
            extractorName = extractor
        )
    }
}
