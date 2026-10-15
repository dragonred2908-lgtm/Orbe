package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GeminiModels
import com.example.data.model.ProjectRecord
import com.example.data.repository.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IaStudentAppTest {

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("IA Student", appName)
    }

    @Test
    fun testGeminiModelsAvailable() {
        val models = GeminiModels.ALL
        assertTrue(models.any { it.id == "gemini-4-argon" })
        assertTrue(models.any { it.id == "gemini-3.8-flash" })
        assertTrue(models.any { it.id == "gemini-3.1-flash-lite" })
    }

    @Test
    fun testProjectRepositorySaveAndRetrieve() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProjectRepository(context)

        val project = ProjectRecord(
            title = "Test AI App",
            appType = "Chatbot",
            targetAudience = "Estudiantes",
            naturalLanguage = "Español",
            externalConnections = "Ninguna",
            chosenOption = "Opción 1",
            systemPrompt = "Eres un tutor",
            model = "gemini-3.8-flash"
        )

        repo.saveProject(project)
        val saved = repo.getSavedProjects()
        assertTrue(saved.any { it.title == "Test AI App" })
    }

    @Test
    fun testOrbStateEnum() {
        val states = com.example.ui.orb.OrbState.values()
        assertEquals(3, states.size)
        assertTrue(states.contains(com.example.ui.orb.OrbState.REPOSO))
        assertTrue(states.contains(com.example.ui.orb.OrbState.ESCUCHA))
        assertTrue(states.contains(com.example.ui.orb.OrbState.PROCESAMIENTO))
    }

    @Test
    fun testAssistantSettingsRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AssistantSettingsRepository(context)

        // Default settings verification - NOT Jarvis
        val defaultSettings = repo.settings.value
        assertEquals("Aura", defaultSettings.assistantName)
        assertEquals("Oye Aura", defaultSettings.wakePhrase)
        assertNotEquals("Jarvis", defaultSettings.assistantName)

        // Customization
        repo.updateSettings(
            AssistantSettings(
                assistantName = "Nova",
                wakePhrase = "Oye Nova",
                personality = "Cálido y amigable",
                voiceName = "Aoede"
            )
        )

        val updated = repo.settings.value
        assertEquals("Nova", updated.assistantName)
        assertEquals("Oye Nova", updated.wakePhrase)

        // Wake phrase matching tests
        assertTrue(repo.isWakePhraseMatched("Oye Nova cómo estás"))
        assertTrue(repo.isWakePhraseMatched("¡oye nova!"))
        assertFalse(repo.isWakePhraseMatched("Hola asistente"))
    }

    @Test
    fun testCrossPlatformRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = CrossPlatformRepository(context)

        assertNotNull(repo.syncCode.value)
        assertTrue(repo.syncCode.value.startsWith("AI-"))

        val devices = repo.linkedDevices.value
        assertTrue(devices.isNotEmpty())
        assertTrue(devices.any { it.platform == PlatformType.ANDROID })

        repo.addLinkedDevice("Laptop Oficina", PlatformType.WINDOWS, "192.168.1.180", "C:\\Work")
        val updatedDevices = repo.linkedDevices.value
        assertTrue(updatedDevices.any { it.name == "Laptop Oficina" })

        val script = repo.getPythonDesktopCompanionScript()
        assertTrue(script.contains("RemoteSearchHandler"))
        assertTrue(script.contains("PORT = 8080"))
    }

    @Test
    fun testBusinessAssistantRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = BusinessAssistantRepository(context)

        assertFalse(repo.settings.value.isCommercialModeActive)
        repo.toggleCommercialMode()
        assertTrue(repo.settings.value.isCommercialModeActive)

        repo.addCatalogItem("Consultoría IA", "$150", "Asesoría especializada")
        assertTrue(repo.settings.value.catalogItems.any { it.name == "Consultoría IA" })

        val prompt = repo.buildCommercialSystemPrompt()
        assertTrue(prompt.contains("Asistente Virtual Comercial"))
        assertTrue(prompt.contains("Consultoría IA"))

        repo.recordAutoReply("¿Tienen citas hoy?", "Sí, atendemos hasta las 7pm", "WhatsApp")
        assertTrue(repo.replyLogs.value.any { it.customerQuery.contains("citas") })
    }

    @Test
    fun testFileSearchRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val searchRepo = FileSearchRepository(context)
        val crossRepo = CrossPlatformRepository(context)

        val results = searchRepo.searchLocalAndRemote(
            query = "pdf",
            selectedCategory = "Documentos",
            customPath = null,
            includeRemoteDevices = true,
            remoteDevices = crossRepo.linkedDevices.value
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.extension.equals("PDF", ignoreCase = true) })
    }

    @Test
    fun testPermissionsRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PermissionsRepository(context)

        val initial = repo.permissions.value
        assertTrue(initial.size >= 7)
        assertTrue(repo.isPermissionGranted("perm_microphone"))

        repo.togglePermission("perm_microphone")
        assertFalse(repo.isPermissionGranted("perm_microphone"))

        repo.resetAllPermissions(true)
        assertTrue(repo.isPermissionGranted("perm_microphone"))
    }
}
