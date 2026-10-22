package com.example.data.permission

import kotlinx.coroutines.flow.StateFlow

/**
 * Estado del permiso de audio (RECORD_AUDIO) para el asistente.
 */
enum class AudioPermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUESTED
}

/**
 * Servicio de permisos para gestionar el acceso a RECORD_AUDIO
 * y entrada de micrófono en Android.
 */
interface AudioPermissionService {
    val status: StateFlow<AudioPermissionStatus>

    fun hasPermission(): Boolean

    fun requestPermission(onResult: (Boolean) -> Unit = {})
}
