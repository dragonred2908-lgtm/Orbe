package com.example.shared.audio

import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Grabador de audio para escritorio (Windows, Linux, macOS) utilizando Java Sound API.
 * Captura audio a 16kHz, 16-bit Mono, ideal para modelos de Speech-to-Text.
 */
class JvmAudioRecorder(
    private val sampleRate: Float = 16000f,
    private val onRmsLevel: (Float) -> Unit = {}
) {
    private var line: TargetDataLine? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    private val pcmStream = ByteArrayOutputStream()

    val format = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        sampleRate,
        16, // bits per sample
        1,  // channels (mono)
        2,  // frame size
        sampleRate,
        false // little endian
    )

    fun isMicrophoneSupported(): Boolean {
        return try {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            AudioSystem.isLineSupported(info)
        } catch (e: Throwable) {
            false
        }
    }

    @Synchronized
    fun startRecording(): Boolean {
        if (isRecording.get()) return true
        pcmStream.reset()

        return try {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) {
                return false
            }

            val targetLine = AudioSystem.getLine(info) as TargetDataLine
            targetLine.open(format)
            targetLine.start()
            this.line = targetLine
            isRecording.set(true)

            recordingThread = Thread({
                val buffer = ByteArray(2048)
                while (isRecording.get()) {
                    val bytesRead = targetLine.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        pcmStream.write(buffer, 0, bytesRead)
                        val rms = WavUtils.calculateRms(buffer, bytesRead)
                        onRmsLevel(rms)
                    }
                }
            }, "JvmAudioRecorderThread").apply {
                isDaemon = true
                start()
            }

            true
        } catch (e: Throwable) {
            isRecording.set(false)
            false
        }
    }

    @Synchronized
    fun stopRecording(): ByteArray {
        if (!isRecording.get()) {
            return WavUtils.createWavFile(pcmStream.toByteArray(), sampleRate.toInt())
        }

        isRecording.set(false)
        try {
            line?.stop()
            line?.close()
        } catch (e: Throwable) {
            // Ignorar errores al cerrar línea de audio
        }
        line = null

        try {
            recordingThread?.join(500)
        } catch (e: Throwable) {
            // Ignorar interrupción
        }
        recordingThread = null

        onRmsLevel(0f)
        val rawPcm = pcmStream.toByteArray()
        return WavUtils.createWavFile(rawPcm, sampleRate.toInt())
    }

    fun isCurrentlyRecording(): Boolean = isRecording.get()
}
