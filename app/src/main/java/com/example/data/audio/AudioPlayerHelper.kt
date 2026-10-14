package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudioBytes(bytes: ByteArray, onCompletion: () -> Unit = {}) {
        stop()
        try {
            val tempFile = File.createTempFile("tts_audio_", ".wav", context.cacheDir)
            val fos = FileOutputStream(tempFile)
            fos.write(bytes)
            fos.close()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    onCompletion()
                    tempFile.delete()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error playing audio bytes", e)
        }
    }

    fun playBase64Audio(base64Audio: String, onCompletion: () -> Unit = {}) {
        try {
            val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
            playAudioBytes(bytes, onCompletion)
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error decoding base64 audio", e)
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error stopping audio", e)
        }
    }
}
