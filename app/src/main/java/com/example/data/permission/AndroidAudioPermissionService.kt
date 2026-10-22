package com.example.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación Android de [AudioPermissionService] para gestionar
 * el permiso en tiempo de ejecución android.permission.RECORD_AUDIO.
 */
class AndroidAudioPermissionService(
    private val context: Context
) : AudioPermissionService {

    private val _status = MutableStateFlow(determineStatus())
    override val status: StateFlow<AudioPermissionStatus> = _status.asStateFlow()

    fun updateStatus(isGranted: Boolean) {
        _status.value = if (isGranted) AudioPermissionStatus.GRANTED else AudioPermissionStatus.DENIED
    }

    override fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        val granted = hasPermission()
        updateStatus(granted)
        onResult(granted)
    }

    fun refreshStatus() {
        _status.value = determineStatus()
    }

    private fun determineStatus(): AudioPermissionStatus {
        return if (hasPermission()) AudioPermissionStatus.GRANTED else AudioPermissionStatus.NOT_REQUESTED
    }
}
