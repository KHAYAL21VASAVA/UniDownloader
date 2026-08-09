package com.unidownloader.app.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.unidownloader.app.data.model.DownloadHistoryItem
import com.unidownloader.app.utils.FileNameUtils
import com.unidownloader.app.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DownloadStorage(private val context: Context) {

    companion object {
        private const val TAG = "DownloadStorage"
        private const val APP_SUB_FOLDER = "UniDownloader"
    }

    /**
     * Gets temporary working directory for stream caching and FFmpeg processing.
     */
    fun getTempCacheDir(): File {
        val dir = File(context.cacheDir, "unidownloader_temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Cleans up stale temp files from previous runs to prevent conflicts and free disk space.
     */
    fun cleanupStaleTempFiles() {
        try {
            val dir = getTempCacheDir()
            val files = dir.listFiles() ?: return
            val now = System.currentTimeMillis()
            for (file in files) {
                // Delete files older than 1 hour or incomplete parts
                if (now - file.lastModified() > 3600_000L || file.name.endsWith(".part") || file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Stale cache cleanup note: ${e.message}")
        }
    }

    /**
     * Gets public Downloads folder on device.
     */
    fun getPublicDownloadsDirectory(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    /**
     * Saves a temporary file directly into standard public Downloads folder.
     * Uses MediaStore on Android 10+ (API 29+) with IS_PENDING flag for atomic completion.
     * Returns Pair<File, Uri?> containing absolute file and content URI.
     */
    suspend fun saveToPublicDownloads(
        sourceTempFile: File,
        desiredTitle: String,
        extension: String,
        sourceUrl: String = ""
    ): Pair<File, Uri?> = withContext(Dispatchers.IO) {
        val sanitizedName = FileNameUtils.buildFileName(desiredTitle, extension)
        val mimeType = FormatUtils.getMimeTypeFromExtension(extension)
        val publicDownloadsDir = getPublicDownloadsDirectory()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ Scoped Storage: All files in public Downloads MUST use MediaStore.Downloads collection
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            var itemUri: Uri? = null

            try {
                itemUri = resolver.insert(collection, contentValues)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { output ->
                        sourceTempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }

                    // Finalize entry
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)

                    val targetFile = File(publicDownloadsDir, sanitizedName)
                    scanFile(targetFile)
                    Log.i(TAG, "Saved to MediaStore Downloads: $itemUri -> ${targetFile.absolutePath}")
                    Pair(targetFile, itemUri)
                } else {
                    // Fallback to direct file copy
                    saveViaDirectFile(sourceTempFile, publicDownloadsDir, sanitizedName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore insert failed, falling back to direct file write: ${e.message}")
                if (itemUri != null) {
                    try { resolver.delete(itemUri, null, null) } catch (_: Exception) {}
                }
                saveViaDirectFile(sourceTempFile, publicDownloadsDir, sanitizedName)
            }
        } else {
            // Legacy Android 9 and below
            saveViaDirectFile(sourceTempFile, publicDownloadsDir, sanitizedName)
        }
    }

    private fun saveViaDirectFile(sourceTempFile: File, publicDownloadsDir: File, sanitizedName: String): Pair<File, Uri?> {
        val targetFile = FileNameUtils.getUniqueFile(publicDownloadsDir, sanitizedName)
        sourceTempFile.copyTo(targetFile, overwrite = true)
        scanFile(targetFile)
        return Pair(targetFile, Uri.fromFile(targetFile))
    }

    /**
     * Scans completed file into device's media scanner so Gallery/VLC/Files detects it immediately.
     */
    fun scanFile(file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(FormatUtils.getMimeTypeFromExtension(file.extension))
            ) { path, uri ->
                Log.d(TAG, "Media scanner completed for $path -> $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Media scanner warning: ${e.message}")
        }
    }

    /**
     * Deletes a downloaded file from disk and MediaStore.
     */
    suspend fun deleteDownloadedFile(item: DownloadHistoryItem): Boolean = withContext(Dispatchers.IO) {
        var deleted = false
        try {
            // Delete via content URI if present
            if (item.fileUriString.isNotBlank() && item.fileUriString.startsWith("content://")) {
                val uri = Uri.parse(item.fileUriString)
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deleted = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore delete note: ${e.message}")
        }

        try {
            // Delete via direct file path
            val file = File(item.filePath)
            if (file.exists()) {
                val fileDeleted = file.delete()
                if (fileDeleted) deleted = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct file delete note: ${e.message}")
        }
        deleted
    }
}
