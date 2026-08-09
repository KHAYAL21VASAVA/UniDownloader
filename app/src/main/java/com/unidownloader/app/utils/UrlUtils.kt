package com.unidownloader.app.utils

import java.net.URI

object UrlUtils {

    fun isValidMediaUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return try {
            val uri = URI(trimmed)
            !uri.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    fun cleanUrl(rawUrl: String): String {
        var clean = rawUrl.trim()
        // Strip tracking parameters if necessary
        if (clean.contains("?utm_") || clean.contains("&utm_")) {
            clean = clean.replace(Regex("[?&]utm_[^&]+"), "")
        }
        return clean
    }

    fun detectPlatform(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: ""
            when {
                host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube"
                host.contains("instagram.com") -> "Instagram"
                host.contains("twitter.com") || host.contains("x.com") -> "Twitter / X"
                host.contains("tiktok.com") -> "TikTok"
                host.contains("facebook.com") || host.contains("fb.watch") -> "Facebook"
                host.contains("reddit.com") || host.contains("redd.it") -> "Reddit"
                host.contains("vimeo.com") -> "Vimeo"
                host.contains("soundcloud.com") -> "SoundCloud"
                host.contains("dailymotion.com") -> "Dailymotion"
                host.contains("twitch.tv") -> "Twitch"
                host.contains("pinterest.com") -> "Pinterest"
                host.contains("threads.net") -> "Threads"
                url.endsWith(".mp4", ignoreCase = true) ||
                url.endsWith(".mkv", ignoreCase = true) ||
                url.endsWith(".webm", ignoreCase = true) ||
                url.endsWith(".mp3", ignoreCase = true) ||
                url.endsWith(".m4a", ignoreCase = true) ||
                url.endsWith(".opus", ignoreCase = true) -> "Direct Media File"
                else -> host.ifBlank { "Web Media" }
            }
        } catch (e: Exception) {
            "Web Media"
        }
    }
}
