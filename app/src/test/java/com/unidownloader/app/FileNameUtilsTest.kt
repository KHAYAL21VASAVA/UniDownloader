package com.unidownloader.app

import com.unidownloader.app.utils.FileNameUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameUtilsTest {

    @Test
    fun testSanitizeFileName_removesIllegalCharacters() {
        val raw = "Amazing: Travel / Video * 2026? <Official> | \"4K\""
        val sanitized = FileNameUtils.sanitizeFileName(raw)
        assertEquals("Amazing_ Travel _ Video _ 2026_ _Official_ _ _4K_", sanitized)
    }

    @Test
    fun testSanitizeFileName_emptyFallback() {
        val sanitized = FileNameUtils.sanitizeFileName("   ")
        assertEquals("UniDownloader_Media", sanitized)
    }

    @Test
    fun testBuildFileName() {
        val result = FileNameUtils.buildFileName("My Cool Song", "mp3")
        assertEquals("My Cool Song.mp3", result)

        val resultWithDot = FileNameUtils.buildFileName("Cool Video", ".MP4")
        assertEquals("Cool Video.mp4", resultWithDot)
    }

    @Test
    fun testGetExtension() {
        assertEquals("mp4", FileNameUtils.getExtension("video.mp4"))
        assertEquals("mp3", FileNameUtils.getExtension("song.MP3"))
        assertEquals("mkv", FileNameUtils.getExtension("movie", "mkv"))
    }
}
