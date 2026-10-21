package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ProjectRecord
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ia_student_projects", Context.MODE_PRIVATE)

    fun getSavedProjects(): List<ProjectRecord> {
        val jsonStr = try {
            prefs.getString("projects_list", null)
        } catch (e: Exception) {
            null
        } ?: return getInitialSeedProjects()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ProjectRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ProjectRecord(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        appType = obj.optString("appType"),
                        targetAudience = obj.optString("targetAudience"),
                        naturalLanguage = obj.optString("naturalLanguage"),
                        externalConnections = obj.optString("externalConnections"),
                        chosenOption = obj.optString("chosenOption"),
                        systemPrompt = obj.optString("systemPrompt"),
                        model = obj.optString("model", "gemini-3.8-flash"),
                        temperature = obj.optDouble("temperature", 0.7).toFloat(),
                        topP = obj.optDouble("topP", 0.95).toFloat(),
                        topK = obj.optInt("topK", 40),
                        maxTokens = obj.optInt("maxTokens", 2048),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            // Solo usa la lista de semillas la primera vez (sin preferencias creadas).
            // Si el usuario borró todos sus proyectos ("[]"), respeta esa lista vacía.
            list
        } catch (e: Exception) {
            // JSON corrupto: restaurar semillas y sobrescribir el estado inválido.
            getInitialSeedProjects().also { saveAll(it) }
        }
    }

    fun saveProject(project: ProjectRecord) {
        val current = getSavedProjects().toMutableList()
        current.removeAll { it.id == project.id }
        current.add(0, project)
        saveAll(current)
    }

    fun deleteProject(id: String) {
        val current = getSavedProjects().toMutableList()
        current.removeAll { it.id == id }
        saveAll(current)
    }

    private fun saveAll(list: List<ProjectRecord>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("appType", item.appType)
            obj.put("targetAudience", item.targetAudience)
            obj.put("naturalLanguage", item.naturalLanguage)
            obj.put("externalConnections", item.externalConnections)
            obj.put("chosenOption", item.chosenOption)
            obj.put("systemPrompt", item.systemPrompt)
            obj.put("model", item.model)
            obj.put("temperature", item.temperature.toDouble())
            obj.put("topP", item.topP.toDouble())
            obj.put("topK", item.topK)
            obj.put("maxTokens", item.maxTokens)
            obj.put("createdAt", item.createdAt)
            arr.put(obj)
        }
        prefs.edit().putString("projects_list", arr.toString()).apply()
    }

    private fun getInitialSeedProjects(): List<ProjectRecord> {
        return listOf(
            ProjectRecord(
                id = "seed-1",
                title = "Asistente de Soporte Técnico Especializado",
                appType = "Chatbot con memoria de contexto",
                targetAudience = "Usuarios finales de servicio web",
                naturalLanguage = "Español amigable, conciso y estructurado",
                externalConnections = "Base de datos de tickets de soporte",
                chosenOption = "Chatbot conversacional con System Instructions estrictas",
                systemPrompt = "Eres un agente de soporte técnico experto. Responde siempre con tono empático, pide capturas de pantalla si es necesario y ofrece pasos numerados de solución.",
                model = "gemini-3.8-flash",
                temperature = 0.4f,
                topP = 0.9f,
                topK = 40,
                maxTokens = 1024
            ),
            ProjectRecord(
                id = "seed-2",
                title = "Analizador de Código & Auditor de Seguridad",
                appType = "Herramienta de análisis profundo",
                targetAudience = "Desarrolladores Senior",
                naturalLanguage = "Técnico, preciso, Markdown con bloques de código",
                externalConnections = "Repositorios GitHub / Archivos locales",
                chosenOption = "Flujo estructurado con Modo Pensamiento Alto",
                systemPrompt = "Eres un auditor senior de ciberseguridad. Analiza el código buscando vulnerabilidades (OWASP Top 10), sugiere parches inmediatos y justifica cada recomendación.",
                model = "gemini-4-argon",
                temperature = 0.2f,
                topP = 0.95f,
                topK = 40,
                maxTokens = 4096
            )
        )
    }
}
