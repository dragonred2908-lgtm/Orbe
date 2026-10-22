package com.example.shared.audio

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sintetizador de voz (Text-to-Speech) OFFLINE usando las voces del propio sistema:
 *  - Windows: voz SAPI5 integrada (System.Speech) vía PowerShell — gratis, sin descargas.
 *  - macOS: comando nativo `say`.
 *  - Linux: `espeak-ng`, `espeak` o `spd-say` si están instalados; si no, silencio.
 *
 * Ideal para equipos antiguos: no consume red ni GPU.
 */
object JvmTtsSpeaker {

    @Volatile
    private var lastSpokeAt = 0L

    /**
     * Habla el texto dado en un hilo demonio (no bloquea la UI).
     * Los errores se tragan: hablar nunca debe romper el asistente.
     */
    fun speak(text: String, force: Boolean = false) {
        val clean = sanitizeForSpeech(text)
        if (clean.isBlank()) return

        // Anti-eco: no repetir la misma frase en menos de 800 ms
        val now = System.currentTimeMillis()
        if (!force && now - lastSpokeAt < 800) return
        lastSpokeAt = now

        val thread = Thread({
            try {
                when {
                    isWindows() -> speakWindowsSapi(clean)
                    isMac() -> runCatching { ProcessBuilder("say", clean).start().waitFor(30, TimeUnit.SECONDS) }
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
     * Windows: usa la voz SAPI del sistema. El texto viaja por STDIN para
     * evitar cualquier problema de quoting en la línea de comandos.
     */
    private fun speakWindowsSapi(text: String) {
        val command = """
            Add-Type -AssemblyName System.Speech;
            ${'$'}synth = New-Object System.Speech.Synthesis.SpeechSynthesizer;
            ${'$'}text = [Console]::In.ReadToEnd();
            ${'$'}synth.Speak(${'$'}text)
        """.trimIndent()

        val process = ProcessBuilder(
            "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command
        ).redirectErrorStream(true).start()

        process.outputStream.bufferedWriter().use { it.write(text) }
        process.waitFor(30, TimeUnit.SECONDS)
        if (process.isAlive) process.destroyForcibly()
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
