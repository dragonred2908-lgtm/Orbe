package com.example.shared.audio

import java.util.concurrent.TimeUnit

/**
 * Sintetizador de voz (Text-to-Speech) OFFLINE usando las voces del propio sistema:
 *  - Windows: voz SAPI5 integrada (System.Speech) vía PowerShell — gratis, sin descargas.
 *    Se selecciona automáticamente la mejor voz en español instalada
 *    (p. ej. "Microsoft Helena" / "Sabina" / "Laura"), nunca la voz inglesa
 *    por defecto que pronuncia mal el texto español.
 *  - macOS: comando nativo `say`.
 *  - Linux: `espeak-ng`, `espeak` o `spd-say` si están instalados; si no, silencio.
 *
 * Ideal para equipos antiguos: no consume red ni GPU.
 */
object JvmTtsSpeaker {

    @Volatile
    private var lastSpokeAt = 0L

    @Volatile
    private var cachedVoiceName: String? = null

    /** Proceso de voz en curso: se cancela al empezar una locución nueva. */
    @Volatile
    private var activeProcess: Process? = null

    @Volatile
    private var voiceResolved = false

    /** Permite silenciar la voz desde la UI (menú hamburguesa). */
    @Volatile
    var enabled: Boolean = true

    /**
     * Habla el texto dado en un hilo demonio (no bloquea la UI).
     * Los errores se tragan: hablar nunca debe romper el asistente.
     */
    fun speak(text: String, force: Boolean = false) {
        if (!enabled) return

        val clean = sanitizeForSpeech(text)
        if (clean.isBlank()) return

        // Anti-eco: no repetir la misma frase en menos de 800 ms
        val now = System.currentTimeMillis()
        if (!force && now - lastSpokeAt < 800) return
        lastSpokeAt = now

        // Cortar la locución anterior: evita respuestas encoladas o solapadas
        runCatching { activeProcess?.destroyForcibly() }
        activeProcess = null

        val thread = Thread({
            try {
                when {
                    isWindows() -> speakWindowsSapi(clean)
                    isMac() -> {
                        val process = ProcessBuilder("say", clean).start()
                        activeProcess = process
                        process.waitFor(30, TimeUnit.SECONDS)
                        if (process.isAlive) process.destroyForcibly()
                    }
                    else -> speakLinuxEspeak(clean)
                }
            } catch (_: Exception) {
                // Silencio ante cualquier fallo de la voz del sistema.
            }
        }, "TtsSpeaker").apply {
            isDaemon = true
            start()
        }
    }

    /** Quita marcas de formato tipo Markdown que arruinarían la locución. */
    private fun sanitizeForSpeech(text: String): String {
        return text
            .replace(Regex("""[*_#`>|-]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(600)
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("win")

    private fun isMac(): Boolean =
        System.getProperty("os.name").lowercase().contains("mac")

    /**
     * Windows: usa la mejor voz SAPI en español disponible en el sistema.
     * El texto viaja en Base64 como argumento: así no hay problemas de quoting
     * ni de codificación (los acentos sobreviven intactos).
     */
    private fun speakWindowsSapi(text: String) {
        val voice = resolveBestSpanishVoice()
        val selectVoiceClause = if (voice != null) {
            val safeName = voice.replace("'", "''")
            "\$synth.SelectVoice('$safeName');"
        } else {
            ""
        }

        val encoded = java.util.Base64.getEncoder()
            .encodeToString(text.toByteArray(Charsets.UTF_8))

        val command = """
            Add-Type -AssemblyName System.Speech;
            ${'$'}synth = New-Object System.Speech.Synthesis.SpeechSynthesizer;
            $selectVoiceClause
            ${'$'}synth.Rate = 0;
            ${'$'}synth.Volume = 100;
            ${'$'}text = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String('$encoded'));
            ${'$'}synth.Speak(${'$'}text)
        """.trimIndent()

        val process = ProcessBuilder(
            "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command
        ).redirectErrorStream(true).start()

        activeProcess = process
        // Consumir la salida para que el proceso no se bloquee por Buffer lleno.
        process.inputStream.bufferedReader().readText()
        process.waitFor(60, TimeUnit.SECONDS)
        if (process.isAlive) process.destroyForcibly()
        if (activeProcess === process) activeProcess = null
    }

    /**
     * Enumera las voces SAPI instaladas una sola vez y elige la mejor en español.
     * Preferencia: Helena > Sabina > Laura > Pablo > Raúl > cualquier voz es-*
     * (nunca la voz inglesa por defecto).
     */
    private fun resolveBestSpanishVoice(): String? {
        if (voiceResolved) return cachedVoiceName
        voiceResolved = true

        try {
            val command = """
                Add-Type -AssemblyName System.Speech;
                ${'$'}s = New-Object System.Speech.Synthesis.SpeechSynthesizer;
                ${'$'}s.GetInstalledVoices() | ForEach-Object { ${'$'}_.VoiceInfo.Name }
            """.trimIndent()

            val process = ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(10, TimeUnit.SECONDS)

            val names = output.lines().map { it.trim() }.filter { it.isNotBlank() }
            val priority = listOf("helena", "sabina", "laura", "pablo", "raul", "raúl", "spanish", "español")
            cachedVoiceName = names.firstOrNull { name ->
                val lower = name.lowercase()
                priority.any { lower.contains(it) }
            } ?: names.firstOrNull { it.lowercase().contains("spanish") || it.lowercase().contains("espa") }
        } catch (_: Exception) {
            cachedVoiceName = null
        }

        return cachedVoiceName
    }

    private fun speakLinuxEspeak(text: String) {
        for (binary in listOf("espeak-ng", "espeak", "spd-say")) {
            try {
                val process = ProcessBuilder(binary, text).start()
                if (process.waitFor(30, TimeUnit.SECONDS)) return
            } catch (_: Exception) {
                // Probar el siguiente binario
            }
        }
    }
}
