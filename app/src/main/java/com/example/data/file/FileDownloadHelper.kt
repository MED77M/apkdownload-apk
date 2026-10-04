package com.example.data.file

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat

object FileDownloadHelper {

    private const val TAG = "FileDownloadHelper"

    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = sizeInBytes / Math.pow(1024.0, index.toDouble())
        return "${DecimalFormat("#,##0.#").format(value)} ${units[index]}"
    }

    suspend fun downloadAndSaveToPhone(
        context: Context,
        urlOrData: String,
        suggestedFileName: String,
        mimeType: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeFileName = sanitizeFileName(suggestedFileName, mimeType)
            val cacheFolder = File(context.cacheDir, "downloads").apply { if (!exists()) mkdirs() }
            val cachedFile = File(cacheFolder, safeFileName)

            val bytes = when {
                urlOrData.startsWith("data:") -> {
                    val base64Data = urlOrData.substringAfter("base64,")
                    Base64.decode(base64Data, Base64.DEFAULT)
                }
                urlOrData.startsWith("http://") || urlOrData.startsWith("https://") -> {
                    val connection = URL(urlOrData).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 25000
                    connection.instanceFollowRedirects = true
                    connection.connect()
                    if (connection.responseCode in 200..299) {
                        connection.inputStream.use { it.readBytes() }
                    } else {
                        throw Exception("HTTP Error: ${connection.responseCode}")
                    }
                }
                urlOrData.startsWith("content://") -> {
                    val uri = Uri.parse(urlOrData)
                    val directBytes = try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (sec: SecurityException) {
                        Log.w(TAG, "Picker URI access expired for $urlOrData: ${sec.message}")
                        null
                    } catch (e: Exception) {
                        Log.w(TAG, "Error opening content URI $urlOrData: ${e.message}")
                        null
                    }

                    if (directBytes != null && directBytes.isNotEmpty()) {
                        directBytes
                    } else {
                        // Attempt fallback to Coil's memory cache if available without re-triggering openInputStream
                        val coilBytes = try {
                            val bitmap = coil.Coil.imageLoader(context).memoryCache?.get(coil.memory.MemoryCache.Key(urlOrData))?.bitmap
                            if (bitmap != null) {
                                val stream = java.io.ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                                stream.toByteArray()
                            } else null
                        } catch (_: Exception) {
                            null
                        }

                        coilBytes ?: throw Exception("هذا الملف مرسل بإصدار قديم كمسار مؤقت غير متاح على هذا الهاتف. يرجى إرساله مجدداً ليتم حفظه سحابياً.")
                    }
                }
                urlOrData.startsWith("file://") -> {
                    val path = Uri.parse(urlOrData).path ?: urlOrData.removePrefix("file://")
                    File(path).readBytes()
                }
                File(urlOrData).exists() -> {
                    File(urlOrData).readBytes()
                }
                else -> {
                    // Try decoding as raw base64 if possible
                    try {
                        Base64.decode(urlOrData, Base64.DEFAULT)
                    } catch (_: Exception) {
                        throw Exception("Unsupported file format or source")
                    }
                }
            }

            // 1. Cache copy for FileProvider opening
            cachedFile.writeBytes(bytes)

            // 2. Save directly into device Public Downloads folder (so user finds it in Downloads app)
            saveToPublicDownloads(context, safeFileName, mimeType, bytes)

            Result.success(cachedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading file: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun saveToPublicDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collectionUri = when {
                    mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val relativePath = when {
                    mimeType.startsWith("image/") -> Environment.DIRECTORY_PICTURES + "/ScienceEST"
                    mimeType.startsWith("audio/") -> Environment.DIRECTORY_MUSIC + "/ScienceEST"
                    mimeType.startsWith("video/") -> Environment.DIRECTORY_MOVIES + "/ScienceEST"
                    else -> Environment.DIRECTORY_DOWNLOADS + "/ScienceEST"
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                val uri = context.contentResolver.insert(collectionUri, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(bytes)
                        outputStream.flush()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ScienceEST")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeBytes(bytes)
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Public downloads write note: ${e.message}")
        }
    }

    fun openFile(context: Context, file: File, mimeType: String): Result<Unit> {
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching viewer for ${file.name}", e)
            Result.failure(e)
        }
    }

    private fun sanitizeFileName(fileName: String, mimeType: String): String {
        var clean = fileName.trim().replace(Regex("[^a-zA-Z0-9._\\- \u0600-\u06FF]"), "_")
        if (clean.isBlank()) clean = "file_${System.currentTimeMillis()}"

        val hasExtension = clean.contains(".") && clean.substringAfterLast(".").length in 2..5
        if (!hasExtension) {
            val ext = when {
                mimeType.contains("pdf") -> ".pdf"
                mimeType.contains("audio") || mimeType.contains("m4a") -> ".m4a"
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                mimeType.contains("png") -> ".png"
                mimeType.contains("word") || mimeType.contains("doc") -> ".docx"
                mimeType.contains("excel") || mimeType.contains("sheet") -> ".xlsx"
                mimeType.contains("text") -> ".txt"
                else -> ""
            }
            clean += ext
        }
        return clean
    }
}
