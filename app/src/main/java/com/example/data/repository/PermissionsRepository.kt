package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppPermissionItem(
    val key: String,
    val title: String,
    val description: String,
    val category: String, // "Hardware", "Datos", "Automatización", "Red"
    val isEnabled: Boolean,
    val iconEmoji: String
)

class PermissionsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_permissions_prefs", Context.MODE_PRIVATE)

    private val _permissions = MutableStateFlow(loadPermissions())
    val permissions: StateFlow<List<AppPermissionItem>> = _permissions.asStateFlow()

    private fun loadPermissions(): List<AppPermissionItem> {
        return listOf(
            AppPermissionItem(
                key = "perm_microphone",
                title = "Micrófono & Escucha en Vivo",
                description = "Permite reconocer tu frase de llamada personalizada y recibir comandos por voz.",
                category = "Hardware",
                isEnabled = prefs.getBoolean("perm_microphone", true),
                iconEmoji = "🎙️"
            ),
            AppPermissionItem(
                key = "perm_files",
                title = "Exploración de Archivos Locales",
                description = "Permite al asistente buscar, indexar y mostrar documentos y descargas en este equipo.",
                category = "Datos",
                isEnabled = prefs.getBoolean("perm_files", true),
                iconEmoji = "📂"
            ),
            AppPermissionItem(
                key = "perm_remote_pc",
                title = "Interconexión Remota PC (Windows / Mac / Linux)",
                description = "Permite conectar nodos de escritorio para buscar archivos y sincronizar tareas a distancia.",
                category = "Red",
                isEnabled = prefs.getBoolean("perm_remote_pc", true),
                iconEmoji = "💻"
            ),
            AppPermissionItem(
                key = "perm_commercial",
                title = "Automatización Comercial de Mensajes",
                description = "Autoriza al asistente a responder consultas comerciales según catálogo y horarios establecidos.",
                category = "Automatización",
                isEnabled = prefs.getBoolean("perm_commercial", true),
                iconEmoji = "💼"
            ),
            AppPermissionItem(
                key = "perm_media_studio",
                title = "Creación y Edición de Fotos/Videos",
                description = "Habilita la generación de imágenes, edición visual y creación de clips de video con IA.",
                category = "Datos",
                isEnabled = prefs.getBoolean("perm_media_studio", true),
                iconEmoji = "🎨"
            ),
            AppPermissionItem(
                key = "perm_web_search",
                title = "Búsqueda Web en Tiempo Real",
                description = "Permite al asistente consultar información actualizada de internet mediante Gemini.",
                category = "Red",
                isEnabled = prefs.getBoolean("perm_web_search", true),
                iconEmoji = "🌐"
            ),
            AppPermissionItem(
                key = "perm_speech_synthesis",
                title = "Síntesis de Voz (Audio Output)",
                description = "Permite al asistente hablar fluidamente con voces neuronales de Google.",
                category = "Hardware",
                isEnabled = prefs.getBoolean("perm_speech_synthesis", true),
                iconEmoji = "🔊"
            )
        )
    }

    fun togglePermission(key: String) {
        val current = _permissions.value
        val updated = current.map {
            if (it.key == key) {
                val newVal = !it.isEnabled
                prefs.edit().putBoolean(key, newVal).apply()
                it.copy(isEnabled = newVal)
            } else it
        }
        _permissions.value = updated
    }

    fun isPermissionGranted(key: String): Boolean {
        return _permissions.value.find { it.key == key }?.isEnabled ?: true
    }

    fun resetAllPermissions(enabled: Boolean = true) {
        val updated = _permissions.value.map {
            prefs.edit().putBoolean(it.key, enabled).apply()
            it.copy(isEnabled = enabled)
        }
        _permissions.value = updated
    }
}
