package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ProductCatalogItem(
    val id: String,
    val name: String,
    val price: String,
    val description: String,
    val inStock: Boolean = true
)

data class CommercialAutoReplyLog(
    val id: String,
    val customerQuery: String,
    val autoReply: String,
    val channel: String, // "WhatsApp", "Web", "Mensajes", "Soporte"
    val timestamp: String
)

data class BusinessSettings(
    val isCommercialModeActive: Boolean = false,
    val businessName: String = "Mi Negocio Digital",
    val businessCategory: String = "Servicios y Consultoría",
    val businessHours: String = "Lunes a Viernes de 9:00 AM a 6:00 PM",
    val contactPhone: String = "+1 555-0199",
    val welcomeMessage: String = "¡Hola! Gracias por comunicarte con nosotros. ¿En qué podemos ayudarte hoy?",
    val autoReplyRules: String = "Responder con amabilidad y precisión basándote en nuestro catálogo de productos y servicios. Si piden agendar una cita o presupuesto especial, indicar que un asesor se comunicará en breve.",
    val catalogItems: List<ProductCatalogItem> = listOf(
        ProductCatalogItem("1", "Plan Básico Asistente IA", "$49/mes", "Acceso a consultas ilimitadas y soporte básico"),
        ProductCatalogItem("2", "Plan Empresarial Pro", "$149/mes", "Integración multiequipo PC y móvil, automatización y reportes"),
        ProductCatalogItem("3", "Consultoría Personalizada", "$80/hora", "Sesión 1 a 1 de optimización de flujos de trabajo")
    )
)

class BusinessAssistantRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("business_assistant_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<BusinessSettings> = _settings.asStateFlow()

    private val _replyLogs = MutableStateFlow<List<CommercialAutoReplyLog>>(loadLogs())
    val replyLogs: StateFlow<List<CommercialAutoReplyLog>> = _replyLogs.asStateFlow()

    private fun loadSettings(): BusinessSettings {
        val isActive = prefs.getBoolean("comm_active", false)
        val name = prefs.getString("comm_name", "Mi Empresa / Negocio") ?: "Mi Empresa / Negocio"
        val category = prefs.getString("comm_category", "Comercio & Servicios") ?: "Comercio & Servicios"
        val hours = prefs.getString("comm_hours", "Lunes a Sábado: 8:00 AM - 7:00 PM") ?: "Lunes a Sábado: 8:00 AM - 7:00 PM"
        val phone = prefs.getString("comm_phone", "+1 (555) 012-3456") ?: "+1 (555) 012-3456"
        val welcome = prefs.getString("comm_welcome", "¡Hola! Te damos la bienvenida. Soy el asistente virtual comercial. ¿En qué podemos ayudarte hoy?") ?: ""
        val rules = prefs.getString("comm_rules", "Responder dudas de precios, disponibilidad y características usando el catálogo registrado.") ?: ""

        val catalogRaw = prefs.getString("comm_catalog_json", null)
        val catalog = if (!catalogRaw.isNullOrBlank()) {
            try {
                val arr = JSONArray(catalogRaw)
                val list = mutableListOf<ProductCatalogItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        ProductCatalogItem(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            price = o.getString("price"),
                            description = o.getString("description"),
                            inStock = o.getBoolean("inStock")
                        )
                    )
                }
                list
            } catch (e: Exception) {
                defaultCatalog()
            }
        } else {
            defaultCatalog()
        }

        return BusinessSettings(
            isCommercialModeActive = isActive,
            businessName = name,
            businessCategory = category,
            businessHours = hours,
            contactPhone = phone,
            welcomeMessage = welcome,
            autoReplyRules = rules,
            catalogItems = catalog
        )
    }

    private fun defaultCatalog(): List<ProductCatalogItem> = listOf(
        ProductCatalogItem("1", "Servicio Estándar", "$50.00", "Atención rápida y garantía de servicio"),
        ProductCatalogItem("2", "Paquete Premium", "$120.00", "Soporte 24/7 y personalización completa"),
        ProductCatalogItem("3", "Asesoría Express", "$25.00", "Diagnóstico inmediato de necesidades")
    )

    private fun loadLogs(): List<CommercialAutoReplyLog> {
        val raw = prefs.getString("comm_logs_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CommercialAutoReplyLog>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    CommercialAutoReplyLog(
                        id = o.getString("id"),
                        customerQuery = o.getString("customerQuery"),
                        autoReply = o.getString("autoReply"),
                        channel = o.getString("channel"),
                        timestamp = o.getString("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateSettings(newSettings: BusinessSettings) {
        val catalogArr = JSONArray()
        newSettings.catalogItems.forEach { item ->
            val o = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("price", item.price)
                put("description", item.description)
                put("inStock", item.inStock)
            }
            catalogArr.put(o)
        }

        prefs.edit()
            .putBoolean("comm_active", newSettings.isCommercialModeActive)
            .putString("comm_name", newSettings.businessName)
            .putString("comm_category", newSettings.businessCategory)
            .putString("comm_hours", newSettings.businessHours)
            .putString("comm_phone", newSettings.contactPhone)
            .putString("comm_welcome", newSettings.welcomeMessage)
            .putString("comm_rules", newSettings.autoReplyRules)
            .putString("comm_catalog_json", catalogArr.toString())
            .apply()

        _settings.value = newSettings
    }

    fun toggleCommercialMode() {
        val updated = _settings.value.copy(isCommercialModeActive = !_settings.value.isCommercialModeActive)
        updateSettings(updated)
    }

    fun addCatalogItem(name: String, price: String, description: String) {
        val newItem = ProductCatalogItem(
            id = "cat_" + System.currentTimeMillis(),
            name = name,
            price = price,
            description = description,
            inStock = true
        )
        val updatedList = _settings.value.catalogItems + newItem
        updateSettings(_settings.value.copy(catalogItems = updatedList))
    }

    fun removeCatalogItem(id: String) {
        val updatedList = _settings.value.catalogItems.filter { it.id != id }
        updateSettings(_settings.value.copy(catalogItems = updatedList))
    }

    fun recordAutoReply(query: String, reply: String, channel: String = "Simulador Comercial") {
        val newLog = CommercialAutoReplyLog(
            id = "log_" + System.currentTimeMillis(),
            customerQuery = query,
            autoReply = reply,
            channel = channel,
            timestamp = "Hoy, " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        )
        val current = _replyLogs.value.toMutableList()
        current.add(0, newLog)
        val limited = current.take(50)

        val arr = JSONArray()
        limited.forEach { l ->
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("customerQuery", l.customerQuery)
                put("autoReply", l.autoReply)
                put("channel", l.channel)
                put("timestamp", l.timestamp)
            })
        }
        prefs.edit().putString("comm_logs_json", arr.toString()).apply()
        _replyLogs.value = limited
    }

    fun buildCommercialSystemPrompt(): String {
        val s = _settings.value
        val catalogText = s.catalogItems.joinToString("\n") {
            "- ${it.name} | Precio: ${it.price} | Detalle: ${it.description} (Disponible: ${if (it.inStock) "Sí" else "No"})"
        }
        return """
Eres el Asistente Virtual Comercial oficial de "${s.businessName}" (${s.businessCategory}).
Horario de Atención: ${s.businessHours}.
Teléfono / Contacto: ${s.contactPhone}.

Instrucciones y Reglas de Negocio:
${s.autoReplyRules}

Catálogo de Productos y Servicios Disponibles:
$catalogText

Reglas de respuesta:
1. Responde de forma muy profesional, cordial y orientada a cerrar ventas o solucionar dudas.
2. Si preguntan por precios o productos, usa la información del catálogo.
3. Si solicitan información fuera del horario o un presupuesto especial, ofrece tomar sus datos y confirma que el equipo humano se comunicará.
4. Mantén las respuestas claras, concisas y listas para enviar por chat comercial (WhatsApp / SMS / Web).
        """.trimIndent()
    }
}
