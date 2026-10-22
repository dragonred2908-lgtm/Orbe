package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GeminiApiClient
import com.example.data.permission.AndroidAudioPermissionService
import com.example.data.permission.AudioPermissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioPermissionAndApiTest {

    @Test
    fun testAndroidAudioPermissionServiceInitialState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = AndroidAudioPermissionService(context)

        assertNotNull(service.status.value)
        val hasPerm = service.hasPermission()
        if (hasPerm) {
            assertEquals(AudioPermissionStatus.GRANTED, service.status.value)
        } else {
            assertEquals(AudioPermissionStatus.NOT_REQUESTED, service.status.value)
        }
    }

    @Test
    fun testAndroidAudioPermissionServiceUpdateStatus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = AndroidAudioPermissionService(context)

        service.updateStatus(true)
        assertEquals(AudioPermissionStatus.GRANTED, service.status.value)

        service.updateStatus(false)
        assertEquals(AudioPermissionStatus.DENIED, service.status.value)
    }

    @Test
    fun testAndroidAudioPermissionServiceRequest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = AndroidAudioPermissionService(context)

        var callbackCalled = false
        service.requestPermission {
            callbackCalled = true
        }
        assertTrue(callbackCalled)
    }

    @Test
    fun testGeminiApiClientKeyManagement() {
        val client = GeminiApiClient()

        // Configuración de clave en caliente para pruebas
        client.setUserApiKey("test_mock_api_key_12345")
        assertTrue(client.isApiKeyConfigured())

        client.setUserApiKey("")
    }

    @Test
    fun testGeminiApiTestConnectionWithoutKey() = runBlocking {
        val client = GeminiApiClient()
        client.setUserApiKey("") // Sin clave

        val result = client.testApiConnection("")
        assertFalse(result.isSuccess)
        assertTrue(result.message.contains("no configurada") || result.message.contains("Ajustes"))
    }

    @Test
    fun testGeminiApiTranscribeAudioEmptyBufferValidation() = runBlocking {
        val client = GeminiApiClient()
        client.setUserApiKey("test_key")

        val emptyResult = client.transcribeAudio(ByteArray(0))
        assertTrue(emptyResult.contains("vacío") || emptyResult.contains("Error"))
    }

    @Test
    fun testGeminiApiVoiceModelCompatibility() {
        val available = com.example.data.model.GeminiModels.ALL
        assertTrue(available.any { it.id == "gemini-3.8-flash" })
        assertTrue(available.any { it.id == "gemini-3.1-flash-lite" })
    }
}
