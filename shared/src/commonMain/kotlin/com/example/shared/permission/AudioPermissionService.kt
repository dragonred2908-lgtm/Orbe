package com.example.shared.permission

import kotlinx.coroutines.flow.StateFlow

/**
 * Estado del permiso de audio (micrófono) en Compose Multiplatform.
 */
enum class AudioPermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUESTED
}

/**
 * Servicio unificado de permisos para gestionar el acceso a RECORD_AUDIO
 * en Android y verificación de hardware de audio en Desktop.
 */
interface AudioPermissionService {
    val status: StateFlow<AudioPermissionStatus>

    fun hasPermission(): Boolean

    fun requestPermission(onResult: (Boolean) -> Unit = {})
}
