package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AssistantSettings(
    val assistantName: String = "Aura",
    val wakePhrase: String = "Oye Aura",
    val personality: String = "Profesional, conciso y de alta tecnología",
    val voiceName: String = "Kore", // Kore, Aoede, Fenrir, Puck, Charon
    val autoListenEnabled: Boolean = false,
    val customGreeting: String = "Sistemas en línea y a su completa disposición.",
    val matchContains: Boolean = true // True: match if spoken text contains wake phrase
)

class AssistantSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("assistant_preferences", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private fun loadSettings(): AssistantSettings {
        return AssistantSettings(
            assistantName = prefs.getString(KEY_NAME, "Aura") ?: "Aura",
            wakePhrase = prefs.getString(KEY_WAKE_PHRASE, "Oye Aura") ?: "Oye Aura",
            personality = prefs.getString(KEY_PERSONALITY, "Profesional, conciso y de alta tecnología")
                ?: "Profesional, conciso y de alta tecnología",
            voiceName = prefs.getString(KEY_VOICE, "Kore") ?: "Kore",
            autoListenEnabled = prefs.getBoolean(KEY_AUTO_LISTEN, false),
            customGreeting = prefs.getString(KEY_GREETING, "Sistemas en línea y a su completa disposición.")
                ?: "Sistemas en línea y a su completa disposición.",
            matchContains = prefs.getBoolean(KEY_MATCH_CONTAINS, true)
        )
    }

    fun updateSettings(newSettings: AssistantSettings) {
        prefs.edit()
            .putString(KEY_NAME, newSettings.assistantName.trim())
            .putString(KEY_WAKE_PHRASE, newSettings.wakePhrase.trim())
            .putString(KEY_PERSONALITY, newSettings.personality.trim())
            .putString(KEY_VOICE, newSettings.voiceName)
            .putBoolean(KEY_AUTO_LISTEN, newSettings.autoListenEnabled)
            .putString(KEY_GREETING, newSettings.customGreeting.trim())
            .putBoolean(KEY_MATCH_CONTAINS, newSettings.matchContains)
            .apply()

        _settings.value = newSettings
    }

    fun isWakePhraseMatched(spokenText: String): Boolean {
        val current = _settings.value
        val normalizedSpoken = normalizeString(spokenText)
        val normalizedWake = normalizeString(current.wakePhrase)

        if (normalizedWake.isEmpty()) return false

        return if (current.matchContains) {
            normalizedSpoken.contains(normalizedWake)
        } else {
            normalizedSpoken == normalizedWake
        }
    }

    private fun normalizeString(text: String): String {
        return text.lowercase()
            .replace(Regex("[áàäâ]"), "a")
            .replace(Regex("[éèëê]"), "e")
            .replace(Regex("[íìïî]"), "i")
            .replace(Regex("[óòöô]"), "o")
            .replace(Regex("[úùüû]"), "u")
            .replace(Regex("[,.¡!¿?\"':;\\-_()]"), "")
            .trim()
    }

    companion object {
        private const val KEY_NAME = "key_assistant_name"
        private const val KEY_WAKE_PHRASE = "key_wake_phrase"
        private const val KEY_PERSONALITY = "key_personality"
        private const val KEY_VOICE = "key_voice"
        private const val KEY_AUTO_LISTEN = "key_auto_listen"
        private const val KEY_GREETING = "key_greeting"
        private const val KEY_MATCH_CONTAINS = "key_match_contains"

        val PRESET_VOICES = listOf("Kore", "Aoede", "Fenrir", "Puck", "Charon")
        val PRESET_NAMES = listOf("Aura", "Nova", "Lumina", "Atlas", "Kore", "Nexus", "Orion")
        val PRESET_PERSONALITIES = listOf(
            "Profesional, conciso y de alta tecnología",
            "Cálido, amigable y servicial",
            "Científico, analítico y preciso",
            "Ejecutivo, directo y orientado a soluciones"
        )
    }
}
