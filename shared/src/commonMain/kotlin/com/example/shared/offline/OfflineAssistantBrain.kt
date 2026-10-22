package com.example.shared.offline

import com.example.shared.stt.WakePhraseDetector

/**
 * Cerebro del asistente en modo SIN CONEXIÓN (sin GEMINI_API_KEY).
 * Resuelve una lista curada de intenciones en español usando únicamente
 * emparejamiento de palabras clave y aritmética básica: cero red, cero coste,
 * ejecutable en equipos antiguos (CPU sola).
 *
 * La fecha y hora se inyectan como proveedores para que la lógica sea
 * 100% Kotlin común, testeable en CI sin dependencias de plataforma.
 */
class OfflineAssistantBrain(
    private val assistantName: String = "Aura",
    private val wakePhrase: String = "Oye Aura",
    private val currentTimeProvider: () -> String,
    private val currentDateProvider: () -> String
) {

    /**
     * Responde a la consulta del usuario en español. Nunca lanza excepciones.
     */
    fun answer(query: String): String {
        val q = WakePhraseDetector.normalize(query)
        if (q.isBlank()) {
            return "No te escuché bien. Puedes decir, por ejemplo: \"$wakePhrase, ¿qué hora es?\"."
        }

        return when {
            // Hora y fecha
            matchesAny(q, "hora", "que hora", "hora es") && !matchesAny(q, "horario de clase") ->
                "🕐 Son las ${currentTimeProvider()}."

            matchesAny(q, "fecha", "que dia", "dia es hoy", "en que dia estamos") ->
                "📅 Hoy es ${dateProviderSafe()}."

            // Identidad
            matchesAny(q, "quien eres", "como te llamas", "tu nombre", "presentate", "preséntate", "que eres") ->
                "👋 Soy $assistantName, tu asistente educativa de IA Student. " +
                    "Ahora estoy en modo sin conexión: te ayudo con hora, fecha, cálculos básicos y dudas sobre la app. " +
                    "Para preguntas abiertas, añade una GEMINI_API_KEY en Ajustes y pasaré a modo en línea."

            // Ayuda / capacidades
            matchesAny(q, "ayuda", "que puedes hacer", "comandos", "funciones", "que sabes hacer", "para que sirves") ->
                buildString {
                    appendLine("🧩 En modo sin conexión puedo ayudarte con esto:")
                    appendLine("• \"$wakePhrase, ¿qué hora es?\"")
                    appendLine("• \"$wakePhrase, ¿qué fecha es hoy?\"")
                    appendLine("• \"$wakePhrase, ¿cuánto es 25 por 4?\" (cálculos básicos)")
                    appendLine("• \"$wakePhrase, ¿quién eres?\"")
                    appendLine("• Saludos y despedidas 😊")
                    append("Si configuras tu GEMINI_API_KEY, desbloqueo consultas abiertas con Gemini.")
                }

            // Clima (honesta: necesita red/API)
            matchesAny(q, "clima", "tiempo hace", "temperatura", "va a llover", "pronostico") ->
                "🌤️ El clima requiere consulta en línea. Configura tu GEMINI_API_KEY en Ajustes y pregúntame de nuevo."

            // Despedidas y cortesía
            matchesAny(q, "gracias", "muchas gracias", "adios", "hasta luego", "nos vemos") ->
                "💜 ¡De nada! Estaré aquí. Di \"$wakePhrase\" cuando me necesites."

            // Saludos
            matchesAny(q, "hola", "buenos dias", "buenas tardes", "buenas noches", "que tal", "hey") ->
                "👋 ¡Hola! Soy $assistantName en modo sin conexión. ¿Qué hora es, una fecha, o un cálculo? Di \"$wakePhrase, ayuda\" para la lista completa."

            // Cálculo aritmético básico
            else -> evaluateBasicMath(q)?.let { result ->
                "🧮 El resultado es: $result"
            } ?: unknownAnswer(query)
        }
    }

    private fun unknownAnswer(originalQuery: String): String {
        return "🤖 Estoy en modo sin conexión y no reconozco \"$originalQuery\". " +
            "Puedo darte la hora, la fecha, hacer cálculos básicos o contarte qué hace la app. " +
            "Di \"$wakePhrase, ayuda\" para ver todo. " +
            "Si añades una GEMINI_API_KEY en Ajustes, podré responder preguntas abiertas."
    }

    private fun dateProviderSafe(): String = try {
        currentDateProvider()
    } catch (e: Exception) {
        "(fecha no disponible)"
    }

    private fun matchesAny(normalizedText: String, vararg needles: String): Boolean {
        // Normalizamos las agujas también por si traen tildes
        return needles.any { normalizedText.contains(WakePhraseDetector.normalize(it)) }
    }

    /**
     * Detecta y evalúa una operación aritmética simple del tipo
     * "25 por 4", "10 + 5", "100 dividido entre 3", "7 x 8".
     * Devuelve el resultado formateado o null si no hay operación reconocible.
     */
    fun evaluateBasicMath(normalizedText: String): String? {
        val text = normalizeMath(normalizedText)
            .replace(",", ".")
            // Frases largas primero: "dividido por 3" no debe degradarse en "/ * 3"
            .replace("dividido entre", "/")
            .replace("dividido por", "/")
            .replace("dividido", "/")
            .replace("entre", "/")
            .replace("multiplicado por", "*")
            .replace("multiplicado", "*")
            .replace("mas", "+")
            .replace("menos", "-")
            .replace("por", "*")
            .replace("x", "*")
            .replace("elevado a", "^")
            .replace("elevado", "^")

        // Soporta: <num> <op> <num> con signos, decimales y espacios
        val pattern = Regex("""(-?\d+(?:\.\d+)?)\s*([+\-*/^])\s*(-?\d+(?:\.\d+)?)""")
        val match = pattern.find(text) ?: return null

        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val b = match.groupValues[3].toDoubleOrNull() ?: return null

        val raw: Double = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) return "⚠️ No se puede dividir entre cero." else a / b
            "^" -> {
                if (a == 0.0 && b < 0) return "⚠️ Indeterminado: cero elevado a un exponente negativo."
                if (a < 0 && b % 1 != 0.0) return "⚠️ No puedo calcular raíces de números negativos sin complejos."
                try {
                    Math.pow(a, b)
                } catch (e: Exception) {
                    return "⚠️ Resultado fuera de rango."
                }
            }
            else -> return null
        }

        return formatNumber(raw)
    }

    /**
     * Normalización para matemática: minúsculas y sin tildes pero CONSERVANDO
     * los símbolos `+ - * / ^ , .` que [WakePhraseDetector.normalize] eliminaría.
     */
    private fun normalizeMath(text: String): String {
        val folded = buildString {
            for (ch in text.lowercase()) {
                val replaced = when (ch) {
                    'á', 'à', 'ä', 'â' -> 'a'
                    'é', 'è', 'ë', 'ê' -> 'e'
                    'í', 'ì', 'ï', 'î' -> 'i'
                    'ó', 'ò', 'ö', 'ô' -> 'o'
                    'ú', 'ù', 'ü', 'û' -> 'u'
                    'ñ' -> 'n'
                    else -> ch
                }
                append(replaced)
            }
        }
        return folded.replace(Regex("\\s+"), " ").trim()
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            // Máximo 6 decimales significativos sin ceros de relleno
            val rounded = kotlin.math.round(value * 1e6) / 1e6
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }
}
