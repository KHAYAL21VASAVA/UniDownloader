package com.unidownloader.app.utils

import java.io.File

object FileNameUtils {

    private val ILLEGAL_CHARACTERS = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")
    private val RESERVED_NAMES = setOf("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9")

    /**
     * Sanitizes a title string into a safe filesystem filename.
     */
    fun sanitizeFileName(rawTitle: String, fallback: String = "UniDownloader_Media"): String {
        var clean = rawTitle.trim()
            .replace(ILLEGAL_CHARACTERS, "_")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (clean.isBlank() || RESERVED_NAMES.contains(clean.uppercase())) {
            clean = fallback
        }

        // Limit length to avoid exceeding Android/Linux max filename length
        if (clean.length > 150) {
            clean = clean.substring(0, 150).trim()
        }

        return clean.ifBlank { fallback }
    }

    /**
     * Combines sanitized title with extension.
     */
    fun buildFileName(title: String, extension: String): String {
        val cleanTitle = sanitizeFileName(title)
        val cleanExt = extension.trimStart('.').lowercase()
        return "$cleanTitle.$cleanExt"
    }

    /**
     * Generates a collision-free filename in target directory: "Title (1).mp4", "Title (2).mp4", etc.
     */
    fun getUniqueFile(directory: File, desiredFileName: String): File {
        var file = File(directory, desiredFileName)
        if (!file.exists()) return file

        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension
        val extSuffix = if (ext.isNotBlank()) ".$ext" else ""

        var counter = 1
        while (file.exists()) {
            file = File(directory, "$nameWithoutExt ($counter)$extSuffix")
            counter++
        }
        return file
    }

    /**
     * Extracts base name and extension from a given filename.
     */
    fun getExtension(fileName: String, defaultExt: String = "mp4"): String {
        val ext = fileName.substringAfterLast('.', "")
        return if (ext.isNotBlank()) ext.lowercase() else defaultExt.lowercase()
    }
}
