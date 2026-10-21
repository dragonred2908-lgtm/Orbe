package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID

/**
 * Reproductor de audio híbrido resiliente para Android.
 * Soporta:
 * 1. Muestras de audio PCM lineal sin comprimir (Gemini 2.5 Flash TTS a 24,000 Hz) con encapsulación RIFF/WAV.
 * 2. Archivos WAV, MP3 y OGG nativos mediante MediaPlayer con AudioAttributes optimizados para voz.
 * 3. Fallback inmediato de bajo nivel mediante AudioTrack para streaming directo al hardware.
 * 4. Motor de voz nativo TextToSpeech (TTS) integrado para asegurar que el asistente NUNCA quede mudo ante fallos de red o falta de cuota de API.
 */
class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    @Volatile
    private var isPlayingAudio: Boolean = false

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val spanishLocale = Locale("es", "ES")
                    val result = textToSpeech?.setLanguage(spanishLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                    isTtsReady = true
                    Log.d(TAG, "Motor nativo TextToSpeech inicializado en español.")
                } else {
                    Log.w(TAG, "Fallo al inicializar motor nativo TextToSpeech (status: $status).")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando TextToSpeech", e)
        }
    }

    /**
     * Sintetiza y reproduce texto utilizando el motor de voz nativo de Android (TTS).
     * Ideal como respaldo autónomo cuando no hay conexión a internet o la API de Gemini no devuelve audio.
     */
    fun speakText(text: String, onCompletion: () -> Unit = {}) {
        stop()

        if (text.isBlank()) {
            onCompletion()
            return
        }

        // Limpiar sintaxis de markdown o asteriscos para que la voz suene limpia
        val cleanedText = cleanTextForSpeech(text)
        if (cleanedText.isBlank()) {
            onCompletion()
            return
        }

        isPlayingAudio = true

        val tts = textToSpeech
        if (tts == null || !isTtsReady) {
            Log.w(TAG, "TextToSpeech aún no está listo. Reintentando...")
            initTextToSpeech()
            isPlayingAudio = false
            onCompletion()
            return
        }

        val utteranceId = "tts_" + UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                isPlayingAudio = true
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    isPlayingAudio = false
                    onCompletion()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    isPlayingAudio = false
                    onCompletion()
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    isPlayingAudio = false
                    onCompletion()
                }
            }
        })

        try {
            tts.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al invocar speak() en TextToSpeech", e)
            isPlayingAudio = false
            onCompletion()
        }
    }

    /**
     * Reproduce audio sintetizado en bytes. Si los bytes son nulos o vacíos,
     * automáticamente habla el texto de respaldo mediante el motor de voz nativo.
     */
    fun playAudioOrSpeak(
        audioBytes: ByteArray?,
        fallbackText: String,
        onCompletion: () -> Unit = {}
    ) {
        if (audioBytes != null && audioBytes.isNotEmpty()) {
            playAudioBytes(audioBytes, onCompletion = {
                onCompletion()
            })
        } else if (fallbackText.isNotBlank()) {
            speakText(fallbackText, onCompletion)
        } else {
            onCompletion()
        }
    }

    /**
     * Reproduce bytes de audio binario (WAV, PCM, MP3).
     */
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

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }
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
                try { tempFile.delete() } catch (_: Exception) {}
                isPlayingAudio = false
                onCompletion()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayerNative error: what=$what extra=$extra. Activando fallback AudioTrack...")
                cleanUpMediaPlayer()
                try { tempFile.delete() } catch (_: Exception) {}
                fallbackPlayWithAudioTrack(bytes, onCompletion)
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Excepción en MediaPlayer.prepareAsync (${e.message}). Activando fallback AudioTrack...")
            cleanUpMediaPlayer()
            fallbackPlayWithAudioTrack(bytes, onCompletion)
        }
    }

    /**
     * Fallback de bajo nivel con AudioTrack para flujos PCM directos sin depender del códec de medios.
     * Si los bytes contienen cabecera RIFF de 44 bytes, la remueve para evitar 'clicks' o ruido estático.
     */
    private fun fallbackPlayWithAudioTrack(inputBytes: ByteArray, onCompletion: () -> Unit) {
        Thread({
            try {
                val sampleRate = 24000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT

                // Si contiene cabecera RIFF WAV (44 bytes), extraer únicamente las muestras PCM
                val pcmData = if (hasAudioContainerHeader(inputBytes) && inputBytes.size > 44 &&
                    inputBytes[0] == 'R'.code.toByte() && inputBytes[1] == 'I'.code.toByte()) {
                    inputBytes.copyOfRange(44, inputBytes.size)
                } else {
                    inputBytes
                }

                val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = maxOf(minBufferSize, pcmData.size)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
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

                // Escritura progresiva en fragmentos permitiendo cancelación inmediata con stop()
                val chunkSize = 4096
                var offset = 0
                while (isPlayingAudio && offset < pcmData.size) {
                    val toWrite = minOf(chunkSize, pcmData.size - offset)
                    val written = track.write(pcmData, offset, toWrite)
                    if (written <= 0) break
                    offset += written
                }

                if (isPlayingAudio) {
                    val remainingMs = (pcmData.size.toDouble() / (sampleRate * 2) * 50).toLong()
                    Thread.sleep(remainingMs.coerceIn(10L, 200L))
                }

                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
                audioTrack = null
            } catch (ex: Exception) {
                Log.e(TAG, "Error crítico en fallback AudioTrack", ex)
            } finally {
                isPlayingAudio = false
                onCompletion()
            }
        }, "AudioTrackPlaybackThread").start()
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
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        cleanUpMediaPlayer()
        cleanUpAudioTrack()
    }

    fun destroy() {
        stop()
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsReady = false
        } catch (_: Exception) {}
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
         * Limpia textos con asteriscos, numerales y enlaces markdown para que suenen naturales en voz hablada.
         */
        fun cleanTextForSpeech(input: String): String {
            return input
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // **bold**
                .replace(Regex("\\*(.*?)\\*"), "$1")       // *italic*
                .replace(Regex("#+\\s*"), "")              // # Encabezados
                .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // [texto](url)
                .replace(Regex("`{1,3}.*?`{1,3}"), "")     // `código`
                .replace(Regex("[•\\-–—]"), "")            // viñetas
                .trim()
        }

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
