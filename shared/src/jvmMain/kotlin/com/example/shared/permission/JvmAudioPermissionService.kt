package com.example.shared.permission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

/**
 * Implementación de escritorio de [AudioPermissionService].
 * En JVM (Windows, macOS, Linux), el permiso se gestiona mediante
 * la detección del subsistema de entrada de audio del sistema operativo.
 */
class JvmAudioPermissionService : AudioPermissionService {

    private val _status = MutableStateFlow(detectHardwareSupport())
    override val status: StateFlow<AudioPermissionStatus> = _status.asStateFlow()

    override fun hasPermission(): Boolean {
        return detectHardwareSupport() == AudioPermissionStatus.GRANTED
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        val current = detectHardwareSupport()
        _status.value = current
        onResult(current == AudioPermissionStatus.GRANTED)
    }

    private fun detectHardwareSupport(): AudioPermissionStatus {
        return try {
            val lineInfo = javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, null)
            if (AudioSystem.isLineSupported(lineInfo)) {
                AudioPermissionStatus.GRANTED
            } else {
                AudioPermissionStatus.DENIED
            }
        } catch (_: Throwable) {
            AudioPermissionStatus.GRANTED
        }
    }
}
