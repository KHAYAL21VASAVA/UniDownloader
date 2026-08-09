package com.unidownloader.app

import com.unidownloader.app.utils.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
        assertEquals("1.0 KB", FormatUtils.formatBytes(1024))
        assertEquals("1.5 MB", FormatUtils.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("1.4 GB", FormatUtils.formatBytes((1.42 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", FormatUtils.formatDuration(0))
        assertEquals("01:12", FormatUtils.formatDuration(72))
        assertEquals("01:05:20", FormatUtils.formatDuration(3920))
    }

    @Test
    fun testFormatEta() {
        assertEquals("ETA: --:--", FormatUtils.formatEta(0))
        assertEquals("ETA: 01:12", FormatUtils.formatEta(72))
    }

    @Test
    fun testGetMimeTypeFromExtension() {
        assertEquals("video/mp4", FormatUtils.getMimeTypeFromExtension("mp4"))
        assertEquals("video/x-matroska", FormatUtils.getMimeTypeFromExtension("mkv"))
        assertEquals("audio/mpeg", FormatUtils.getMimeTypeFromExtension("mp3"))
        assertEquals("audio/mp4", FormatUtils.getMimeTypeFromExtension("m4a"))
        assertEquals("audio/opus", FormatUtils.getMimeTypeFromExtension("opus"))
    }
}
