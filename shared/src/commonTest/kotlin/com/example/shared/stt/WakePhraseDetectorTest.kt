package com.example.shared.stt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakePhraseDetectorTest {

    @Test
    fun testNormalizationRemovesAccentsAndPunctuation() {
        val input = "¡Óyé, Aúrá! ¿Cómo estás?"
        val normalized = WakePhraseDetector.normalize(input)
        assertEquals("oye aura como estas", normalized)
    }

    @Test
    fun testWakePhraseMatchingContains() {
        val wake = "Oye Aura"
        assertTrue(WakePhraseDetector.matches("Oye Aura, ¿qué hora es?", wake, true))
        assertTrue(WakePhraseDetector.matches("¡oye aura!", wake, true))
        assertTrue(WakePhraseDetector.matches("hola oye aura dime algo", wake, true))
        assertFalse(WakePhraseDetector.matches("oye siri dime algo", wake, true))
    }

    @Test
    fun testWakePhraseMatchingExact() {
        val wake = "Oye Aura"
        assertTrue(WakePhraseDetector.matches("Oye Aura", wake, false))
        assertTrue(WakePhraseDetector.matches("¡oye aura!", wake, false))
        assertFalse(WakePhraseDetector.matches("Oye Aura, ¿qué hora es?", wake, false))
    }

    @Test
    fun testExtractQueryAfterWakePhrase() {
        val wake = "Oye Aura"
        val query1 = WakePhraseDetector.extractQuery("Oye Aura, ¿qué es la fotosíntesis?", wake)
        assertEquals("¿qué es la fotosíntesis?", query1)

        val query2 = WakePhraseDetector.extractQuery("Oye Aura", wake)
        assertEquals("", query2)

        val query3 = WakePhraseDetector.extractQuery("¡Oye Aura! explícame las leyes de Newton", wake)
        assertEquals("explícame las leyes de Newton", query3)
    }

    @Test
    fun testCustomWakePhrase() {
        val customWake = "IA Student"
        assertTrue(WakePhraseDetector.matches("IA Student, ayuda con matemáticas", customWake, true))
        val query = WakePhraseDetector.extractQuery("IA Student: resuelve este problema", customWake)
        assertEquals("resuelve este problema", query)
    }
}
