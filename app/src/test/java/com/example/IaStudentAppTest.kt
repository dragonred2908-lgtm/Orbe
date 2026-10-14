package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GeminiModels
import com.example.data.model.ProjectRecord
import com.example.data.repository.ProjectRepository
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
}
