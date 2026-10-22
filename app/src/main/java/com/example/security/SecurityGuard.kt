package com.example.security

import android.content.Context
import android.os.Build
import java.io.File
import java.util.regex.Pattern

/**
 * Módulo de Protección y Seguridad de IA Student.
 * 
 * Proporciona:
 * 1. Sanitización de prompts y detección de ataques de inyección (Prompt Injection).
 * 2. Anonimización preventiva de datos de identificación personal (PII) antes de enviar a LLMs.
 * 3. Validación y enmascaramiento seguro de claves de API (Gemini / Google AI Studio).
 * 4. Verificación de entorno seguro y detección de riesgos en el dispositivo.
 */
object SecurityGuard {

    // Patrones de inyección de prompts conocidos y comandos de evasión
    private val PROMPT_INJECTION_PATTERNS = listOf(
        Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("olvida\\s+(todas\\s+las\\s+)?instrucciones\\s+(previas|anteriores)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s+prompt\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("revela\\s+(tu|las)\\s+instrucciones\\s+del\\s+sistema", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+are\\s+now\\s+in\\s+(dan|developer|jailbreak|unrestricted)\\s+mode", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<\\|im_start\\|>|<\\|im_end\\|>|<\\|endoftext\\|>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\\\n|^)###\\s*instruction", Pattern.CASE_INSENSITIVE)
    )

    // Patrones para detectar datos confidenciales o PII (Personally Identifiable Information)
    private val CREDIT_CARD_REGEX = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11})\\b")
    private val EMAIL_REGEX = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")
    private val PHONE_REGEX = Pattern.compile("(\\+?[0-9]{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}")
    private val API_KEY_IN_TEXT_REGEX = Pattern.compile("AIza[0-9A-Za-z\\-_]{35}")

    data class SanitizationResult(
        val cleanText: String,
        val isFlaggedForInjection: Boolean,
        val detectedRisks: List<String>,
        val piiRedactedCount: Int
    )

    /**
     * Sanitiza el texto del usuario antes de enviarlo al modelo de lenguaje.
     * Enmascara datos sensibles y alerta si detecta técnicas de evasión o jailbreak.
     */
    fun sanitizePromptInput(input: String): SanitizationResult {
        if (input.isBlank()) {
            return SanitizationResult(
                cleanText = "",
                isFlaggedForInjection = false,
                detectedRisks = emptyList(),
                piiRedactedCount = 0
            )
        }

        val risks = mutableListOf<String>()
        var flagged = false

        // 1. Detección de patrones de Prompt Injection
        for (pattern in PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                flagged = true
                risks.add("Posible intento de manipulación o sobrescritura de instrucciones del sistema.")
                break
            }
        }

        // 2. Anonimización y enmascaramiento de PII
        var processed = input
        var redactedCount = 0

        val cardMatcher = CREDIT_CARD_REGEX.matcher(processed)
        if (cardMatcher.find()) {
            processed = cardMatcher.replaceAll("[TARJETA_PROTEGIDA]")
            redactedCount++
            risks.add("Se detectaron y protegieron números de tarjeta.")
        }

        val apiKeyMatcher = API_KEY_IN_TEXT_REGEX.matcher(processed)
        if (apiKeyMatcher.find()) {
            processed = apiKeyMatcher.replaceAll("[API_KEY_PROTEGIDA]")
            redactedCount++
            risks.add("Se enmascaró una clave de API expuesta en el texto.")
        }

        val emailMatcher = EMAIL_REGEX.matcher(processed)
        if (emailMatcher.find()) {
            processed = emailMatcher.replaceAll("[EMAIL_PROTEGIDO]")
            redactedCount++
        }

        val phoneMatcher = PHONE_REGEX.matcher(processed)
        if (phoneMatcher.find()) {
            processed = phoneMatcher.replaceAll("[TELÉFONO_PROTEGIDO]")
            redactedCount++
        }

        return SanitizationResult(
            cleanText = processed,
            isFlaggedForInjection = flagged,
            detectedRisks = risks,
            piiRedactedCount = redactedCount
        )
    }

    /**
     * Valida si un string cumple el formato de una API Key de Google Gemini válida.
     */
    fun isValidGeminiApiKeyFormat(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.length < 25) return false
        // Las claves de Google AI Studio / Gemini típicamente comienzan con 'AIzaSy' y tienen ~39 caracteres
        val validPattern = Pattern.compile("^AIza[0-9A-Za-z\\-_]{35,}$")
        return validPattern.matcher(trimmed).matches()
    }

    /**
     * Enmascara de forma segura una clave API para mostrarla en la interfaz sin exponerla.
     * Ejemplo: "AIzaSyD...1234" -> "AIza••••••••••••••••••••1234"
     */
    fun maskApiKey(key: String): String {
        val trimmed = key.trim()
        if (trimmed.length <= 8) {
            return "••••••••"
        }
        val prefix = trimmed.take(4)
        val suffix = trimmed.takeLast(4)
        val maskedMiddle = "•".repeat((trimmed.length - 8).coerceIn(8, 24))
        return "$prefix$maskedMiddle$suffix"
    }

    /**
     * Realiza comprobaciones básicas de integridad del entorno de ejecución (detección de root).
     */
    fun isDeviceEnvironmentSuspicious(context: Context? = null): Boolean {
        // Verificación de huellas de emulación desprotegida o binarios de su
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in rootPaths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Throwable) {
                // Ignorar excepciones de acceso al sistema de archivos
            }
        }

        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
}
