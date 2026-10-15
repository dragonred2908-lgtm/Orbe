package com.example.shared.stt

/**
 * Detector y normalizador de frases de activación personalizadas.
 * Permite reconocer variantes como "Oye Aura", "oye aura!", "¿Oye Aura?", etc.
 */
object WakePhraseDetector {

    /**
     * Normaliza el texto removiendo signos de puntuación, tildes y pasando a minúsculas.
     */
    fun normalize(text: String): String {
        val cleaned = buildString {
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
                if (replaced.isLetterOrDigit() || replaced.isWhitespace()) {
                    append(replaced)
                } else {
                    append(' ')
                }
            }
        }
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Comprueba si el texto hablado contiene o coincide con la frase de activación configurada.
     */
    fun matches(spokenText: String, wakePhrase: String, matchContains: Boolean = true): Boolean {
        val normSpoken = normalize(spokenText)
        val normWake = normalize(wakePhrase)

        if (normWake.isBlank() || normSpoken.isBlank()) return false

        return if (matchContains) {
            normSpoken.contains(normWake)
        } else {
            normSpoken == normWake
        }
    }

    /**
     * Extrae la consulta del usuario que sigue a la frase de activación.
     * Ejemplo: "Oye Aura, ¿qué tiempo hace hoy?" -> "¿qué tiempo hace hoy?"
     * Si la frase de activación es todo lo dicho, devuelve cadena vacía para indicar
     * que el usuario solo dijo el comando de despertar y se debe esperar la consulta.
     */
    fun extractQuery(spokenText: String, wakePhrase: String): String {
        val normWake = normalize(wakePhrase)
        val normSpoken = normalize(spokenText)

        if (normWake.isBlank() || !normSpoken.contains(normWake)) {
            return spokenText.trim()
        }

        // Buscamos la posición donde aparece la frase normalizada en el texto original
        val wakeWords = normWake.split(" ").filter { it.isNotBlank() }
        if (wakeWords.isEmpty()) return spokenText.trim()

        val firstWakeWord = wakeWords.first()
        val lastWakeWord = wakeWords.last()

        val lowerOriginal = spokenText.lowercase()
        val startIndex = lowerOriginal.indexOf(firstWakeWord)
        if (startIndex != -1) {
            val afterStart = spokenText.substring(startIndex)
            val lastWordIndex = afterStart.lowercase().indexOf(lastWakeWord)
            if (lastWordIndex != -1) {
                val queryPart = afterStart.substring(lastWordIndex + lastWakeWord.length)
                val cleaned = queryPart.trimStart(' ', ',', '.', '!', '?', ':', ';', '-')
                return cleaned.trim()
            }
        }

        return ""
    }
}
