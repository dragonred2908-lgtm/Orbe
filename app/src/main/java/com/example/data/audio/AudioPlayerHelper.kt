package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reproductor de audio híbrido resiliente para Android.
 * Admite archivos WAV con cabecera RIFF, MP3, OGG y flujos de audio PCM sin cabecera
 * procedentes de APIs de síntesis de voz (Gemini 2.5 TTS a 24,000 Hz).
 * Incluye fallback automático a AudioTrack ante errores nativos de MediaPlayer.
 */
class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var isPlayingAudio: Boolean = false

    fun playAudioBytes(bytes: ByteArray, onCompletion: () -> Unit = {}) {
        stop()

        if (bytes.isEmpty()) {
            Log.w(TAG, "El búfer de audio recibido está vacío.")
            onCompletion()
            return
        }

        isPlayingAudio = true

        // 1. Verificar si el búfer cuenta con cabecera estándar de contenedor (RIFF, MP3, OGG)
        val playableBytes = if (hasAudioContainerHeader(bytes)) {
            bytes
        } else {
            // Gemini TTS devuelve muestras lineales PCM de 16 bits a 24,000 Hz (mono).
            // Lo encapsulamos con una cabecera canónica RIFF WAV para máxima compatibilidad.
            wrapPcmInWavHeader(bytes, sampleRate = 24000, channels = 1, bitsPerSample = 16)
        }

        // 2. Intentar reproducir con MediaPlayer
        try {
            val tempFile = File.createTempFile("tts_audio_", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(playableBytes)
                fos.flush()
            }

            val player = MediaPlayer()
            mediaPlayer = player

            player.setDataSource(tempFile.absolutePath)
            player.setOnPreparedListener { mp ->
                try {
                    mp.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar MediaPlayer", e)
                    fallbackPlayWithAudioTrack(bytes, onCompletion)
                }
            }
            player.setOnCompletionListener {
                cleanUpMediaPlayer()
                tempFile.delete()
                isPlayingAudio = false
                onCompletion()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayerNative error: what=$what extra=$extra. Activando fallback AudioTrack...")
                cleanUpMediaPlayer()
                tempFile.delete()
                fallbackPlayWithAudioTrack(bytes, onCompletion)
                true
            }

            player.prepare()
        } catch (e: Exception) {
            Log.w(TAG, "Excepción en MediaPlayer.prepare (${e.message}). Activando fallback AudioTrack...")
            cleanUpMediaPlayer()
            fallbackPlayWithAudioTrack(bytes, onCompletion)
        }
    }

    /**
     * Fallback de bajo nivel con AudioTrack para flujos PCM directos sin depender del códec de medios.
     */
    private fun fallbackPlayWithAudioTrack(pcmBytes: ByteArray, onCompletion: () -> Unit) {
        Thread {
            try {
                val sampleRate = 24000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = maxOf(minBufferSize, pcmBytes.size)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()
                track.write(pcmBytes, 0, pcmBytes.size)

                // Esperar a que el búfer termine de sonar
                val durationMs = (pcmBytes.size.toDouble() / (sampleRate * 2) * 1000).toLong()
                Thread.sleep(durationMs.coerceAtLeast(100L))

                track.stop()
                track.release()
                audioTrack = null
            } catch (ex: Exception) {
                Log.e(TAG, "Error crítico en fallback AudioTrack", ex)
            } finally {
                isPlayingAudio = false
                onCompletion()
            }
        }.start()
    }

    fun playBase64Audio(base64Audio: String, onCompletion: () -> Unit = {}) {
        if (base64Audio.isBlank()) {
            onCompletion()
            return
        }
        try {
            val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
            playAudioBytes(bytes, onCompletion)
        } catch (e: Exception) {
            Log.e(TAG, "Error decodificando audio Base64", e)
            onCompletion()
        }
    }

    fun isPlaying(): Boolean = isPlayingAudio

    fun stop() {
        isPlayingAudio = false
        cleanUpMediaPlayer()
        cleanUpAudioTrack()
    }

    private fun cleanUpMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error limpiando MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun cleanUpAudioTrack() {
        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error limpiando AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }

    companion object {
        private const val TAG = "AudioPlayerHelper"

        /**
         * Detecta si los bytes poseen la cabecera mágica de contenedores de audio populares.
         */
        fun hasAudioContainerHeader(bytes: ByteArray): Boolean {
            if (bytes.size < 4) return false

            // RIFF / WAV
            if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
                return true
            }

            // ID3 (MP3 con metadatos)
            if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
                return true
            }

            // Sincronización de trama MP3 (0xFFE0..0xFFFF)
            if (bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0) {
                return true
            }

            // OggS (OGG Vorbis/Opus)
            if (bytes[0] == 'O'.code.toByte() && bytes[1] == 'g'.code.toByte() &&
                bytes[2] == 'g'.code.toByte() && bytes[3] == 'S'.code.toByte()) {
                return true
            }

            return false
        }

        /**
         * Genera un contenedor RIFF WAV estándar para envolver muestras PCM crudas.
         */
        fun wrapPcmInWavHeader(
            pcmData: ByteArray,
            sampleRate: Int = 24000,
            channels: Int = 1,
            bitsPerSample: Int = 16
        ): ByteArray {
            val totalAudioLen = pcmData.size
            val totalDataLen = totalAudioLen + 36
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = channels * bitsPerSample / 8

            val header = ByteArray(44)
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

            // "RIFF"
            buffer.put('R'.code.toByte())
            buffer.put('I'.code.toByte())
            buffer.put('F'.code.toByte())
            buffer.put('F'.code.toByte())
            buffer.putInt(totalDataLen)
            buffer.put('W'.code.toByte())
            buffer.put('A'.code.toByte())
            buffer.put('V'.code.toByte())
            buffer.put('E'.code.toByte())

            // "fmt "
            buffer.put('f'.code.toByte())
            buffer.put('m'.code.toByte())
            buffer.put('t'.code.toByte())
            buffer.put(' '.code.toByte())
            buffer.putInt(16) // Subchunk1Size para PCM
            buffer.putShort(1.toShort()) // Formato PCM = 1
            buffer.putShort(channels.toShort())
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort(blockAlign.toShort())
            buffer.putShort(bitsPerSample.toShort())

            // "data"
            buffer.put('d'.code.toByte())
            buffer.put('a'.code.toByte())
            buffer.put('t'.code.toByte())
            buffer.put('a'.code.toByte())
            buffer.putInt(totalAudioLen)

            val output = ByteArrayOutputStream(44 + pcmData.size)
            output.write(header)
            output.write(pcmData)
            return output.toByteArray()
        }
    }
}
