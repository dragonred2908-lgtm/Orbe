package com.example.shared.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OfflineAssistantBrainTest {

    private fun brain() = OfflineAssistantBrain(
        assistantName = "Aura",
        wakePhrase = "Oye Aura",
        currentTimeProvider = { "14:35" },
        currentDateProvider = { "miércoles, 22 de octubre de 2026" }
    )

    @Test
    fun testHora() {
        val answer = brain().answer("¿Qué hora es?")
        assertTrue(answer.contains("14:35"), "La respuesta debe incluir la hora inyectada: $answer")
    }

    @Test
    fun testFecha() {
        val answer = brain().answer("¿Qué fecha es hoy?")
        assertTrue(answer.contains("22 de octubre"), "La respuesta debe incluir la fecha inyectada: $answer")
    }

    @Test
    fun testSaludo() {
        val answer = brain().answer("Hola, buenos días")
        assertTrue(answer.contains("Hola"), "El saludo debe contestar al usuario: $answer")
    }

    @Test
    fun testIdentidadMencionaModoOffline() {
        val answer = brain().answer("¿Quién eres?")
        assertTrue(answer.contains("Aura"), "Debe presentarse con su nombre: $answer")
        assertTrue(answer.contains("conexión") || answer.contains("GEMINI_API_KEY"), "Debe explicar el modo sin conexión: $answer")
    }

    @Test
    fun testAyudaListaComandos() {
        val answer = brain().answer("¿Qué puedes hacer?")
        assertTrue(answer.contains("hora"), "La ayuda debe listar el comando de hora: $answer")
        assertTrue(answer.contains("cálculo") || answer.contains("calcul"), "La ayuda debe listar cálculos: $answer")
    }

    @Test
    fun testMultiplicacionEnPalabras() {
        val result = brain().evaluateBasicMath("cuánto es 25 por 4")
        assertEquals("100", result)
    }

    @Test
    fun testDivisionConDecimales() {
        val result = brain().evaluateBasicMath("10 dividido por 4")
        assertEquals("2.5", result)
    }

    @Test
    fun testSumaYPotencia() {
        assertEquals("15", brain().evaluateBasicMath("10 + 5"))
        assertEquals("8", brain().evaluateBasicMath("2 elevado a 3"))
    }

    @Test
    fun testDivisionPorCeroEsSegura() {
        val answer = brain().answer("¿Cuánto es 10 entre 0?")
        assertTrue(answer.contains("cero"), "La división por cero debe devolver aviso, no crash: $answer")
    }

    @Test
    fun testMatematicaIntegraEnAnswer() {
        val answer = brain().answer("¿Cuánto es 25 por 4?")
        assertTrue(answer.contains("100"), "answer() debe resolver cálculos: $answer")
    }

    @Test
    fun testConsultaDesconocidaEsHonesta() {
        val answer = brain().answer("Explícame la teoría cuántica de la gravedad")
        assertTrue(answer.contains("sin conexión"), "Debe reconocer sus límites: $answer")
        assertNotNull(answer)
    }

    @Test
    fun testQueryVaciaNoCrashea() {
        val answer = brain().answer("   ")
        assertTrue(answer.isNotBlank())
    }

    @Test
    fun testMatematicaSinOperacionDevuelveNull() {
        assertEquals(null, brain().evaluateBasicMath("hola que tal"))
    }

    @Test
    fun testNumeroEnPalabrasSimple() {
        // Vosk con gramática suele devolver números en palabras
        assertEquals("100", brain().evaluateBasicMath("veinticinco por cuatro"))
        assertEquals("5", brain().evaluateBasicMath("diez menos cinco"))
        assertEquals("30", brain().evaluateBasicMath("diez mas veinte"))
    }

    @Test
    fun testNumeroEnPalabrasCompuesto() {
        assertEquals("40", brain().evaluateBasicMath("treinta y dos mas ocho"))
        assertEquals("30", brain().evaluateBasicMath("ciento veinte entre cuatro"))
        assertEquals("150", brain().evaluateBasicMath("ciento cincuenta menos cero"))
    }

    @Test
    fun testConvertSpanishNumbersNoTocaUnUna() {
        // "un"/"una" no deben convertirse: romperían frases normales
        assertEquals("una pregunta", brain().convertSpanishNumbers("una pregunta"))
        assertEquals("un momento", brain().convertSpanishNumbers("un momento"))
    }

    @Test
    fun testNumeroEnPalabrasSinLaConjuncion() {
        // La gramática de Vosk a veces omite la "y" al transcribir
        assertEquals("40", brain().evaluateBasicMath("treinta dos mas ocho"))
        assertEquals("70", brain().evaluateBasicMath("sesenta cuatro mas seis"))
    }

    @Test
    fun testConvertSpanishNumbersCientos() {
        assertEquals("125", brain().convertSpanishNumbers("ciento veinticinco"))
        assertEquals("1000", brain().convertSpanishNumbers("mil"))
    }
}
