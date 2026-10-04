package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        try {
            val audioDir = File(context.cacheDir, "voice_notes")
            if (!audioDir.exists()) audioDir.mkdirs()

            val outputFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(22050)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            onSuccess(outputFile)
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to start recording", e)
            isRecording = false
            onError(e.message ?: "Failed to start recording")
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentRecordingFile
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to stop recording", e)
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun playAudio(urlOrPath: String, onComplete: () -> Unit) {
        stopPlayback()
        if (urlOrPath.isBlank()) {
            onComplete()
            return
        }

        try {
            val resolvedDataSource: String = when {
                urlOrPath.startsWith("data:") -> {
                    // Decode base64 voice note into cache file
                    val base64Data = urlOrPath.substringAfter("base64,")
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val tempAudioFile = File(context.cacheDir, "play_voice_${System.currentTimeMillis()}.m4a")
                    tempAudioFile.writeBytes(bytes)
                    tempAudioFile.absolutePath
                }
                urlOrPath.startsWith("file://") -> {
                    android.net.Uri.parse(urlOrPath).path ?: urlOrPath.removePrefix("file://")
                }
                urlOrPath.startsWith("content://") -> {
                    val tempAudioFile = File(context.cacheDir, "play_voice_${System.currentTimeMillis()}.m4a")
                    val bytes = try {
                        val uri = android.net.Uri.parse(urlOrPath)
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        Log.w("AudioHelper", "Could not open content URI for audio: ${e.message}")
                        null
                    }
                    if (bytes != null && bytes.isNotEmpty()) {
                        tempAudioFile.writeBytes(bytes)
                        tempAudioFile.absolutePath
                    } else {
                        urlOrPath
                    }
                }
                else -> {
                    urlOrPath
                }
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(resolvedDataSource)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    stopPlayback()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioHelper", "MediaPlayer error: what=$what, extra=$extra")
                    stopPlayback()
                    onComplete()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to initialize playback for audio", e)
            stopPlayback()
            onComplete()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error releasing player", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun releaseAll() {
        if (isRecording) stopRecording()
        stopPlayback()
    }
}
