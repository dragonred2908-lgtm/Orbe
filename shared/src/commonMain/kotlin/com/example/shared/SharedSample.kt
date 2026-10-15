package com.example.shared

/**
 * Shared sample data and logic for IA Student multiplatform module.
 */
object SharedSample {
    const val APP_NAME = "IA Student"
    const val DEFAULT_PLACEHOLDER_MESSAGE = "IA Student - Desktop placeholder"

    fun getWelcomeMessage(platform: String = "Desktop"): String {
        return "Bienvenido a $APP_NAME ($platform)"
    }
}
