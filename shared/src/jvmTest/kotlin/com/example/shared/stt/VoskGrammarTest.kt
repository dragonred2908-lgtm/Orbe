package com.example.shared.stt

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifica que la gramática restringida de Vosk (modo offline) sea un JSON
 * válido y contenga las piezas imprescindibles. Si esto fallara, el motor
 * caería silenciosamente al reconocimiento libre y perderíamos precisión.
 */
class VoskGrammarTest {

    private val grammar = JvmVoskSttEngine.OFFLINE_GRAMMAR

    @Test
    fun testGramaticaEsArrayJson() {
        assertTrue(grammar.trimStart().startsWith("["), "La gramática debe ser un array JSON: $grammar")
        assertTrue(grammar.trimEnd().endsWith("]"), "La gramática debe cerrar el array JSON")
    }

    @Test
    fun testGramaticaContieneFraseDeActivacion() {
        assertTrue(grammar.contains("\"oye aura\""), "Debe incluir la frase de activación")
    }

    @Test
    fun testGramaticaContieneComandosYNúmeros() {
        assertTrue(grammar.contains("\"que hora es\""), "Debe incluir el comando de hora")
        assertTrue(grammar.contains("\"veinticinco\""), "Debe incluir números en palabras")
        assertTrue(grammar.contains("\"mas\"") && grammar.contains("\"por\""), "Debe incluir operadores")
    }

    @Test
    fun testGramaticaIncluyeComodinUnk() {
        assertTrue(grammar.contains("\"[unk]\""), "Sin [unk] las palabras desconocidas romperían la frase")
    }

    @Test
    fun testGramaticaNoTieneComillasSinEscapar() {
        // Cuenta comillas: cada frase aporta 2; el total debe ser par.
        val quotes = grammar.count { it == '"' }
        assertTrue(quotes % 2 == 0, "Número impar de comillas: $quotes")
    }
}
