package com.example.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.collection.LruCache
import java.io.File

object ChatImageHelper {
    private const val TAG = "ChatImageHelper"
    private val bitmapCache = LruCache<String, Bitmap>(60)

    fun decodeBase64Bitmap(dataUriOrBase64: String): Bitmap? {
        if (dataUriOrBase64.isBlank()) return null

        val cacheKey = if (dataUriOrBase64.length > 80) {
            "${dataUriOrBase64.take(40)}_${dataUriOrBase64.takeLast(40)}_${dataUriOrBase64.length}"
        } else {
            dataUriOrBase64
        }

        val cached = bitmapCache[cacheKey]
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return try {
            val base64Data = if (dataUriOrBase64.contains("base64,")) {
                dataUriOrBase64.substringAfter("base64,")
            } else {
                dataUriOrBase64
            }
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                bitmapCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed decoding base64 image: ${e.message}")
            null
        }
    }

    fun getLocalCacheFile(context: Context, key: String, dataUriOrUrl: String): File? {
        return try {
            val cacheFolder = File(context.cacheDir, "chat_images").apply { if (!exists()) mkdirs() }
            val cleanKey = key.ifBlank { dataUriOrUrl.take(30).hashCode().toString() }
            val targetFile = File(cacheFolder, "img_$cleanKey.jpg")
            if (targetFile.exists() && targetFile.length() > 0L) {
                return targetFile
            }
            if (dataUriOrUrl.startsWith("data:")) {
                val base64Data = dataUriOrUrl.substringAfter("base64,")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                targetFile.writeBytes(bytes)
                return targetFile
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error caching image to file: ${e.message}")
            null
        }
    }
}
