package com.example.ui.business

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApiClient
import com.example.data.repository.BusinessAssistantRepository
import com.example.data.repository.ProductCatalogItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAssistantScreen(
    businessRepo: BusinessAssistantRepository,
    apiClient: GeminiApiClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by businessRepo.settings.collectAsState()
    val replyLogs by businessRepo.replyLogs.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Configuración & Catálogo, 1: Simulador en Vivo, 2: Historial
    val tabs = listOf("⚙️ Configuración", "💬 Simulador Auto-Responder", "📋 Historial (${replyLogs.size})")

    // State for simulator
    var testCustomerQuery by remember { mutableStateOf("") }
    var isSimulating by remember { mutableStateOf(false) }
    var simulatedResponse by remember { mutableStateOf("") }

    // State for editing catalog
    var showAddProductDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. BANNER COMERCIAL & SWITCH ACTIVO ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isCommercialModeActive) Color(0xFF0D281E) else Color(0xFF161B22)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (settings.isCommercialModeActive) Color(0xFF2EA043) else Color(0xFF30363D)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (settings.isCommercialModeActive) Color(0xFF2EA043).copy(alpha = 0.2f) else Color(0xFF8B949E).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💼", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Modo Asistente Comercial",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                if (settings.isCommercialModeActive) "🟢 Activo — Respondiendo mensajes según catálogo" else "⚪ Inactivo — Toca para activar auto-responder",
                                color = if (settings.isCommercialModeActive) Color(0xFF7EE787) else Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.isCommercialModeActive,
                        onCheckedChange = { businessRepo.toggleCommercialMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2EA043),
                            checkedTrackColor = Color(0xFF2EA043).copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // --- 2. SELECTOR DE PESTAÑAS ---
        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF161B22),
                contentColor = Color(0xFF00F0FF)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // --- CONTENIDO SEGÚN PESTAÑA ---
        when (activeTab) {
            0 -> {
                // CONFIGURACIÓN DE NEGOCIO Y CATÁLOGO
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Datos del Negocio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            OutlinedTextField(
                                value = settings.businessName,
                                onValueChange = { businessRepo.updateSettings(settings.copy(businessName = it)) },
                                label = { Text("Nombre de la Empresa / Negocio") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = settings.businessHours,
                                onValueChange = { businessRepo.updateSettings(settings.copy(businessHours = it)) },
                                label = { Text("Horario de Atención Comercial") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = settings.contactPhone,
                                onValueChange = { businessRepo.updateSettings(settings.copy(contactPhone = it)) },
                                label = { Text("WhatsApp / Teléfono de Contacto") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = settings.autoReplyRules,
                                onValueChange = { businessRepo.updateSettings(settings.copy(autoReplyRules = it)) },
                                label = { Text("Reglas de Respuesta y Política Comercial") },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Catálogo de Productos y Precios", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(onClick = { showAddProductDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir Ítem", color = Color(0xFF00F0FF), fontSize = 12.sp)
                        }
                    }
                }

                items(settings.catalogItems) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(item.description, color = Color(0xFF8B949E), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF238636))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(item.price, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            IconButton(onClick = { businessRepo.removeCatalogItem(item.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = Color(0xFFF85149))
                            }
                        }
                    }
                }
            }

            1 -> {
                // SIMULADOR EN VIVO DE ATENCIÓN COMERCIAL
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Simulador de Mensaje Entrante",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Escribe una pregunta como cliente y comprueba cómo responde automáticamente el asistente con IA:",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preguntas frecuentes rápidas de prueba
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val quickQueries = listOf("¿Cuánto cuesta el plan pro?", "¿Qué horario tienen?", "¿Hacen presupuestos?")
                                quickQueries.forEach { q ->
                                    Button(
                                        onClick = { testCustomerQuery = q },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D), contentColor = Color(0xFF80D8FF)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(q, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = testCustomerQuery,
                                onValueChange = { testCustomerQuery = it },
                                placeholder = { Text("Mensaje del cliente (ej: Hola, ¿tienen disponibilidad hoy?)", color = Color.Gray, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (testCustomerQuery.isBlank()) return@Button
                                    coroutineScope.launch {
                                        isSimulating = true
                                        val sysPrompt = businessRepo.buildCommercialSystemPrompt()
                                        val fullPrompt = "$sysPrompt\n\nConsulta del cliente: \"$testCustomerQuery\"\nGenera la respuesta comercial adecuada:"
                                        val res = apiClient.generateContent(
                                            prompt = fullPrompt,
                                            model = "gemini-3.8-flash"
                                        )
                                        simulatedResponse = res.text
                                        businessRepo.recordAutoReply(testCustomerQuery, res.text, "Simulador Comercial")
                                        isSimulating = false
                                    }
                                },
                                enabled = testCustomerQuery.isNotBlank() && !isSimulating,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSimulating) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generando respuesta comercial...")
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simular Auto-Respuesta")
                                }
                            }

                            if (simulatedResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D281E)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2EA043)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🤖", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Respuesta Automática Generada", color = Color(0xFF7EE787), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(simulatedResponse, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // HISTORIAL DE MENSAJES ATENDIDOS AUTOMÁTICAMENTE
                if (replyLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📨", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Aún no hay mensajes en el historial", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Usa el simulador para probar y registrar respuestas", color = Color(0xFF8B949E), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(replyLogs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cliente preguntó:", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(log.timestamp, color = Color(0xFF6E7681), fontSize = 9.sp)
                                }
                                Text("\"${log.customerQuery}\"", color = Color(0xFF80D8FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Auto-Respuesta enviada:", color = Color(0xFF2EA043), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(log.autoReply, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: AÑADIR PRODUCTO O SERVICIO AL CATÁLOGO ---
    if (showAddProductDialog) {
        var prodName by remember { mutableStateOf("") }
        var prodPrice by remember { mutableStateOf("$") }
        var prodDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Añadir al Catálogo Comercial", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Nombre del Producto o Servicio") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("Precio o Tarifa (ej: $25.00)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("Descripción y detalles para el cliente") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prodName.isNotBlank()) {
                            businessRepo.addCatalogItem(prodName, prodPrice, prodDesc)
                            showAddProductDialog = false
                            Toast.makeText(context, "Ítem agregado al catálogo", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Cancelar", color = Color(0xFF8B949E))
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }
}
