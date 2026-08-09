package com.unidownloader.app

import com.unidownloader.app.utils.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun testIsValidMediaUrl() {
        assertTrue(UrlUtils.isValidMediaUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlUtils.isValidMediaUrl("http://example.com/video.mp4"))
        assertFalse(UrlUtils.isValidMediaUrl("ftp://example.com/file"))
        assertFalse(UrlUtils.isValidMediaUrl("not-a-valid-url"))
        assertFalse(UrlUtils.isValidMediaUrl(""))
    }

    @Test
    fun testCleanUrl() {
        val dirty = "https://youtube.com/watch?v=123&utm_source=twitter&utm_medium=social"
        val clean = UrlUtils.cleanUrl(dirty)
        assertEquals("https://youtube.com/watch?v=123", clean)
    }

    @Test
    fun testDetectPlatform() {
        assertEquals("YouTube", UrlUtils.detectPlatform("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("YouTube", UrlUtils.detectPlatform("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("Instagram", UrlUtils.detectPlatform("https://www.instagram.com/reel/C8abc123/"))
        assertEquals("Twitter / X", UrlUtils.detectPlatform("https://x.com/user/status/123456"))
        assertEquals("TikTok", UrlUtils.detectPlatform("https://www.tiktok.com/@user/video/123456"))
        assertEquals("Reddit", UrlUtils.detectPlatform("https://reddit.com/r/videos/comments/123"))
        assertEquals("Direct Media File", UrlUtils.detectPlatform("https://example.com/sample.mp4"))
    }
}
