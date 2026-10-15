package com.example.shared.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utilidades para codificar flujos PCM sin comprimir a formato estándar RIFF/WAV.
 */
object WavUtils {

    /**
     * Envuelve muestras PCM mono a 16-bit en una cabecera RIFF WAV estándar.
     */
    fun createWavFile(pcmData: ByteArray, sampleRate: Int = 16000, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // "fmt " sub-chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // SubChunk1Size (16 for PCM)
        buffer.putShort(1.toShort()) // AudioFormat (1 for PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // "data" sub-chunk
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

    /**
     * Calcula el nivel RMS (Root Mean Square) normalizado (0f..1f) de un búfer PCM de 16 bits.
     */
    fun calculateRms(pcmData: ByteArray, bytesRead: Int): Float {
        if (bytesRead < 2) return 0f
        var sum = 0.0
        val sampleCount = bytesRead / 2

        val buffer = ByteBuffer.wrap(pcmData, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            val sample = buffer.short.toDouble() / 32768.0
            sum += sample * sample
        }

        val rms = kotlin.math.sqrt(sum / sampleCount)
        // Escalado y compresión suave para visualización en UI
        val scaled = (rms * 3.5).coerceIn(0.0, 1.0)
        return scaled.toFloat()
    }
}
