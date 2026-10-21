package com.example.shared.audio

import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.SourceDataLine

/**
 * Reproductor de audio nativo para entornos de escritorio (Windows, Linux, macOS).
 * Utiliza Java Sound API para reproducir flujos WAV y PCM procedentes de síntesis de voz.
 */
class JvmAudioPlayer {
    private var currentClip: Clip? = null
    private var currentLine: SourceDataLine? = null
    private val isPlaying = AtomicBoolean(false)

    @Synchronized
    fun playWavBytes(audioBytes: ByteArray, onCompletion: () -> Unit = {}) {
        stop()

        if (audioBytes.isEmpty()) {
            onCompletion()
            return
        }

        Thread({
            try {
                // Verificar si tiene cabecera RIFF WAV
                val isWav = audioBytes.size >= 4 &&
                    audioBytes[0] == 'R'.code.toByte() &&
                    audioBytes[1] == 'I'.code.toByte() &&
                    audioBytes[2] == 'F'.code.toByte() &&
                    audioBytes[3] == 'F'.code.toByte()

                val wavData = if (isWav) audioBytes else WavUtils.createWavFile(audioBytes, sampleRate = 24000)

                val bais = ByteArrayInputStream(wavData)
                val audioStream: AudioInputStream = AudioSystem.getAudioInputStream(bais)
                val format = audioStream.format

                val clip = AudioSystem.getClip()
                currentClip = clip
                isPlaying.set(true)

                clip.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP || event.type == LineEvent.Type.CLOSE) {
                        isPlaying.set(false)
                        try {
                            clip.close()
                        } catch (_: Exception) {}
                        onCompletion()
                    }
                }

                clip.open(audioStream)
                clip.start()
            } catch (e: Exception) {
                // Fallback directo a SourceDataLine para streaming de PCM.
                // Si el audio entrante ya era WAV, se descarta la cabecera para no
                // reproducir los primeros 44 bytes como PCM crudo (click audible).
                val pcmOnly = if (isWav) audioBytes.copyOfRange(minOf(44, audioBytes.size), audioBytes.size) else audioBytes
                try {
                    playViaSourceDataLine(pcmOnly, onCompletion)
                } catch (ex: Exception) {
                    isPlaying.set(false)
                    onCompletion()
                }
            }
        }, "JvmAudioPlayerThread").start()
    }

    private fun playViaSourceDataLine(pcmBytes: ByteArray, onCompletion: () -> Unit) {
        val format = AudioFormat(24000f, 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        currentLine = line
        line.open(format)
        line.start()
        isPlaying.set(true)

        val chunkSize = 4096
        var offset = 0
        while (isPlaying.get() && offset < pcmBytes.size) {
            val toWrite = minOf(chunkSize, pcmBytes.size - offset)
            line.write(pcmBytes, offset, toWrite)
            offset += toWrite
        }

        line.drain()
        line.stop()
        line.close()
        currentLine = null
        isPlaying.set(false)
        onCompletion()
    }

    @Synchronized
    fun stop() {
        isPlaying.set(false)
        try {
            currentClip?.stop()
            currentClip?.close()
        } catch (_: Exception) {}
        currentClip = null

        try {
            currentLine?.stop()
            currentLine?.close()
        } catch (_: Exception) {}
        currentLine = null
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get()
}
