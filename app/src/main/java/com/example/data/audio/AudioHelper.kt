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
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(urlOrPath)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    stopPlayback()
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onComplete()
                    true
                }
            }
        } catch (e: IOException) {
            Log.e("AudioHelper", "Failed to play audio", e)
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
