package com.example.ui.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GeminiAmber
import com.example.ui.theme.GeminiViolet

data class KnowledgeTopic(
    val title: String,
    val summary: String,
    val details: String,
    val category: String,
    val queryForChat: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeHubScreen(
    onSendQueryToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedTopicTitle by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val categories = listOf("Todos", "Conceptos Clave", "Modelos", "AI Studio vs Vertex", "Prompts", "Seguridad")

    val topics = listOf(
        KnowledgeTopic(
            title = "Google AI Studio vs Vertex AI",
            summary = "Entiende cuándo usar AI Studio y cuándo Vertex AI en Google Cloud.",
            details = """
🔹 **Google AI Studio**:
• Diseñado para desarrolladores, creadores y prototipado rápido.
• Interfaz ágil e intuitiva.
• Obtención inmediata de API Keys.
• Nivel gratuito generoso para experimentación y producción de escala media.

🔹 **Vertex AI**:
• Parte de la consola de Google Cloud Enterprise (GCP).
• Pensado para grandes empresas con contratos empresariales, VPC privada, SLAs estrictos y cumplimiento corporativo.
• Facturación ligada directamente a Google Cloud Billing.
            """.trimIndent(),
            category = "AI Studio vs Vertex",
            queryForChat = "Explícame a fondo las diferencias entre Google AI Studio y Vertex AI y cuándo me conviene cada una."
        ),
        KnowledgeTopic(
            title = "Temperatura, Top-P y Top-K explicados",
            summary = "Cómo controlar la creatividad y precisión de las respuestas.",
            details = """
🎯 **Analogías del día a día**:
• **Temperatura (0.0 - 2.0)**: Es el termostato de la creatividad. 
  - 0.0 a 0.3: Precisión matemática, lógica, código. El modelo elige siempre la opción más certera.
  - 0.7: Conversación natural equilibrada.
  - 1.2+: Creatividad alta, poesía, generación de ideas sin filtros.

• **Top-K**: Limita las palabras candidatas a las 'K' más probables antes del muestreo (ejemplo: 40).
• **Top-P (Nucleus Sampling)**: Suma las probabilidades de las mejores palabras hasta alcanzar el porcentaje 'P' (ejemplo: 0.95 = 95%).
            """.trimIndent(),
            category = "Conceptos Clave",
            queryForChat = "Explícame con analogías cotidianas cómo afectan la Temperatura, el Top-P y el Top-K a mi app en AI Studio."
        ),
        KnowledgeTopic(
            title = "System Instructions y Few-Shot Prompting",
            summary = "Cómo dar instrucciones perfectas al modelo para que nunca se desvíe.",
            details = """
💡 **System Instructions**:
Configuración raíz persistente que define la identidad, el rol, el tono y las restricciones del modelo antes de cualquier mensaje del usuario.

📝 **Few-Shot Prompting**:
Enseñar al modelo mostrándole 2 o 3 ejemplos resueltos (pares de Entrada y Salida esperada). Esto reduce drásticamente las alucinaciones y estandariza el formato de salida (por ejemplo, asegurando respuestas en JSON válido).
            """.trimIndent(),
            category = "Prompts",
            queryForChat = "Dame las mejores prácticas para escribir System Instructions y ejemplos Few-Shot en AI Studio."
        ),
        KnowledgeTopic(
            title = "Modelos Gemini: Cuándo usar cada uno",
            summary = "Guía de selección entre Gemini 4 Argon, 3.8 Flash y 3.1 Flash Lite.",
            details = """
🧠 **gemini-4-argon**:
• Máxima capacidad de razonamiento y Modo Pensamiento Alto (ThinkingLevel.HIGH).
• Ideal para auditoría de código, análisis multimodal complejo de imágenes y videos largos.

⚡ **gemini-3.8-flash**:
• El caballo de batalla para uso general.
• Ultra rápido, versátil y compatible con herramientas de Grounding (Google Search y Google Maps).

🚀 **gemini-3.1-flash-lite**:
• Diseñado para respuestas ultra rápidas con latencia mínima y alto volumen de peticiones concurrentes.
            """.trimIndent(),
            category = "Modelos",
            queryForChat = "¿Cuál modelo de Gemini debería elegir para mi caso de uso específico?"
        ),
        KnowledgeTopic(
            title = "Safety Settings (Filtros de Seguridad)",
            summary = "Ajusta los umbrales de contenido para acoso, violencia y contenido explícito.",
            details = """
🛡️ **Filtros en AI Studio**:
Permite configurar el umbral de bloqueo para 4 categorías:
1. Harassment (Acoso)
2. Hate Speech (Incitación al odio)
3. Sexually Explicit (Contenido explícito)
4. Dangerous Content (Contenido peligroso)

Puedes ajustar los umbrales entre BLOCK_NONE, BLOCK_ONLY_HIGH, BLOCK_MEDIUM_AND_ABOVE o BLOCK_LOW_AND_ABOVE según los requisitos de tu producto.
            """.trimIndent(),
            category = "Seguridad",
            queryForChat = "¿Cómo debo configurar los Safety Settings de Gemini en mi proyecto?"
        )
    )

    val filteredTopics = if (selectedCategory == "Todos") topics else topics.filter { it.category == selectedCategory }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Centro de Conocimiento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Guías, conceptos y secretos de Google AI Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTopics) { topic ->
                    val isExpanded = expandedTopicTitle == topic.title

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedTopicTitle = if (isExpanded) null else topic.title
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topic.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topic.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = topic.details,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FilledTonalButton(
                                        onClick = { onSendQueryToChat(topic.queryForChat) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Consultar a IA Student en el Chat")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
