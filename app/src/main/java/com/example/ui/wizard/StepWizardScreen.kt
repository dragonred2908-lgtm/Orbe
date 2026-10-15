package com.example.ui.wizard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApiClient
import com.example.data.model.GeminiModels
import com.example.data.model.ProjectRecord
import com.example.data.repository.ProjectRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepWizardScreen(
    apiClient: GeminiApiClient,
    projectRepository: ProjectRepository,
    onNavigateToChatWithPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }

    // Paso 1 Data
    var projectTitle by remember { mutableStateOf("Mi Asistente Inteligente") }
    var appType by remember { mutableStateOf("Chatbot conversacional") }
    var targetAudience by remember { mutableStateOf("Público general") }
    var naturalLanguage by remember { mutableStateOf("Español amigable, cercano y estructurado") }
    var externalConnections by remember { mutableStateOf("Solo modelo de IA por ahora") }

    // Paso 3 Choice
    var chosenOptionIndex by remember { mutableIntStateOf(0) }
    val optionsList = listOf(
        "Opción 1: Chatbot conversacional simple (ideal para preguntas frecuentes y atención)",
        "Opción 2: Flujo estructurado con pasos guiados (ideal para generadores de contenido y formularios)",
        "Opción 3: Asistente con Herramientas y Conexiones externas (Function Calling / Grounding)",
        "Opción 4: Experiencia multimodal rica (análisis de fotos, voz y documentos)"
    )

    // Paso 4 Data
    var systemPrompt by remember {
        mutableStateOf(
            "Eres un asistente virtual empático y eficiente para $targetAudience. Tu objetivo es guiar en $appType con explicaciones claras, sin jerga innecesaria y con formato Markdown ordenado."
        )
    }
    var selectedModel by remember { mutableStateOf(GeminiModels.FLASH.id) }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.95f) }
    var topK by remember { mutableIntStateOf(40) }
    var maxTokens by remember { mutableIntStateOf(2048) }
    var isGeneratingPrompt by remember { mutableStateOf(false) }
    var selectedCodeTab by remember { mutableIntStateOf(0) } // 0: Python, 1: Kotlin, 2: cURL, 3: JavaScript

    // Paso 5 Data (Sandbox Testing)
    var testInputText by remember { mutableStateOf("Hola, ¿cómo puedes ayudarme hoy?") }
    var testResponseText by remember { mutableStateOf("") }
    var isTestingPrompt by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Constructor Paso a Paso",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Metodología oficial de 5 pasos en AI Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            val project = ProjectRecord(
                                title = projectTitle,
                                appType = appType,
                                targetAudience = targetAudience,
                                naturalLanguage = naturalLanguage,
                                externalConnections = externalConnections,
                                chosenOption = optionsList[chosenOptionIndex],
                                systemPrompt = systemPrompt,
                                model = selectedModel,
                                temperature = temperature,
                                topP = topP,
                                topK = topK,
                                maxTokens = maxTokens
                            )
                            projectRepository.saveProject(project)
                            Toast.makeText(context, "¡Proyecto guardado exitosamente!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_project_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar", fontSize = 12.sp)
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
            // Stepper Indicator Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stepTitles = listOf("1. Idea", "2. Conceptos", "3. Opciones", "4. Código", "5. Pruebas")
                    for (i in 1..5) {
                        val isCurrent = currentStep == i
                        val isDone = currentStep > i

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentStep = i }
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isDone -> SuccessGreen
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = if (!isCurrent && !isDone) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isDone) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(
                                            text = "$i",
                                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stepTitles[i - 1],
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Step Content Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        item {
                            Step1Comprension(
                                projectTitle = projectTitle,
                                onTitleChange = { projectTitle = it },
                                appType = appType,
                                onAppTypeChange = { appType = it },
                                targetAudience = targetAudience,
                                onAudienceChange = { targetAudience = it },
                                naturalLanguage = naturalLanguage,
                                onLanguageChange = { naturalLanguage = it },
                                externalConnections = externalConnections,
                                onConnectionsChange = { externalConnections = it }
                            )
                        }
                    }
                    2 -> {
                        item {
                            Step2Conceptos()
                        }
                    }
                    3 -> {
                        item {
                            Step3Opciones(
                                options = optionsList,
                                selectedIndex = chosenOptionIndex,
                                onSelect = { chosenOptionIndex = it }
                            )
                        }
                    }
                    4 -> {
                        item {
                            Step4Construccion(
                                systemPrompt = systemPrompt,
                                onSystemPromptChange = { systemPrompt = it },
                                selectedModel = selectedModel,
                                onModelChange = { selectedModel = it },
                                temperature = temperature,
                                onTemperatureChange = { temperature = it },
                                topP = topP,
                                onTopPChange = { topP = it },
                                topK = topK,
                                onTopKChange = { topK = it },
                                maxTokens = maxTokens,
                                onMaxTokensChange = { maxTokens = it },
                                isGeneratingPrompt = isGeneratingPrompt,
                                onAutoGeneratePrompt = {
                                    coroutineScope.launch {
                                        isGeneratingPrompt = true
                                        val promptRequest = """
Genera una System Instruction perfecta, profesional y estructurada para Google AI Studio.
Tipo de App: $appType
Público objetivo: $targetAudience
Tono y lenguaje: $naturalLanguage
Conexiones / Herramientas: $externalConnections
Arquitectura elegida: ${optionsList[chosenOptionIndex]}

Sigue el formato: Rol, Objetivo, Reglas de respuesta, Estilo y Formato de salida.
                                        """.trimIndent()
                                        val res = apiClient.generateContent(prompt = promptRequest, model = GeminiModels.FLASH.id)
                                        systemPrompt = res.text
                                        isGeneratingPrompt = false
                                    }
                                },
                                selectedCodeTab = selectedCodeTab,
                                onCodeTabChange = { selectedCodeTab = it },
                                onCopyCode = { code -> copyToClipboard("Código AI Studio", code) }
                            )
                        }
                    }
                    5 -> {
                        item {
                            Step5Pruebas(
                                testInputText = testInputText,
                                onTestInputChange = { testInputText = it },
                                testResponseText = testResponseText,
                                isTesting = isTestingPrompt,
                                onRunTest = {
                                    coroutineScope.launch {
                                        isTestingPrompt = true
                                        val res = apiClient.generateContent(
                                            prompt = testInputText,
                                            model = selectedModel,
                                            systemInstruction = systemPrompt,
                                            temperature = temperature,
                                            topP = topP,
                                            topK = topK,
                                            maxTokens = maxTokens
                                        )
                                        testResponseText = res.text
                                        isTestingPrompt = false
                                    }
                                },
                                onConsultMentor = {
                                    val promptForChat = """
Mentor IA Student, estoy evaluando mi proyecto '$projectTitle' en el Paso 5:
- System Prompt: $systemPrompt
- Entrada de prueba: $testInputText
- Respuesta obtenida: $testResponseText
¿Qué optimizaciones o ajustes a la temperatura y parámetros me sugieres para mejorar?
                                    """.trimIndent()
                                    onNavigateToChatWithPrompt(promptForChat)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Footer (Atrás / Siguiente)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentStep > 1) currentStep-- },
                        enabled = currentStep > 1,
                        modifier = Modifier.testTag("step_back_button")
                    ) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atrás")
                    }

                    Text(
                        text = "Paso $currentStep de 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (currentStep < 5) currentStep++
                        },
                        modifier = Modifier.testTag("step_next_button")
                    ) {
                        Text(if (currentStep == 5) "Finalizar" else "Continuar")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun Step1Comprension(
    projectTitle: String,
    onTitleChange: (String) -> Unit,
    appType: String,
    onAppTypeChange: (String) -> Unit,
    targetAudience: String,
    onAudienceChange: (String) -> Unit,
    naturalLanguage: String,
    onLanguageChange: (String) -> Unit,
    externalConnections: String,
    onConnectionsChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GeminiAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PASO 1 — Comprensión del Proyecto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "En esta primera etapa definimos las bases. Hazte estas 4 preguntas clave recomendadas por IA Student:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = projectTitle,
                onValueChange = onTitleChange,
                label = { Text("Nombre del proyecto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = appType,
                onValueChange = onAppTypeChange,
                label = { Text("¿Qué tipo de app quieres crear? (ej. chatbot, asistente, generador)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = targetAudience,
                onValueChange = onAudienceChange,
                label = { Text("¿Para quién es esta app? (público general, empresa, estudiantes)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = naturalLanguage,
                onValueChange = onLanguageChange,
                label = { Text("¿Qué lenguaje natural y tono usarán los usuarios?") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = externalConnections,
                onValueChange = onConnectionsChange,
                label = { Text("¿Necesita conectarse con datos externos o solo con el modelo?") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun Step2Conceptos() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.School, contentDescription = null, tint = GeminiViolet)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PASO 2 — Explicación de Conceptos con Analogías", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            "Para dominar Google AI Studio, aquí tienes los conceptos explicados con situaciones de la vida cotidiana:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val conceptCards = listOf(
            Triple(
                "📝 System Instructions",
                "Es como el manual de contratación de un empleado: define la personalidad, las reglas estrictas y qué tiene prohibido hacer.",
                "Define la identidad fija antes de cualquier mensaje del usuario."
            ),
            Triple(
                "🌡️ Temperatura (0.0 a 2.0)",
                "Es como el termostato de la creatividad: en 0.0 es un contador calculando impuestos; en 1.5 es un poeta inventando metáforas.",
                "Usa 0.2 para código o datos precisos. Usa 0.7 para conversación. Usa 1.2+ para lluvia de ideas."
            ),
            Triple(
                "🎯 Top-P y Top-K",
                "Top-K es elegir solo entre las 'K' palabras más probables. Top-P es considerar las mejores opciones hasta sumar 'P'% de probabilidad acumulada.",
                "Valores típicos en AI Studio: Top-P = 0.95, Top-K = 40."
            ),
            Triple(
                "🧩 Few-Shot Prompting",
                "Es como enseñarle a alguien mostrando 2 o 3 ejemplos resueltos de examen antes de pedirle que resuelva el siguiente.",
                "En AI Studio puedes agregar pares 'User' / 'Model' de ejemplo en la configuración."
            ),
            Triple(
                "🛠️ Function Calling",
                "Es darle a tu asistente un teléfono para que llame a una API externa (como clima o base de datos) cuando no sabe el dato exacto.",
                "Permite al modelo generar argumentos estructurados para invocar tus funciones locales."
            )
        )

        conceptCards.forEach { (title, analogy, tip) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💡 Analogía: $analogy", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "📌 Tip técnico: $tip",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step3Opciones(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, tint = AiStudioBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PASO 3 — Opciones Sencillas para Avanzar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            "Selecciona una de las 4 rutas arquitectónicas para continuar con tu proyecto:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        options.forEachIndexed { index, optionText ->
            val isSelected = selectedIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(index) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun Step4Construccion(
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    topP: Float,
    onTopPChange: (Float) -> Unit,
    topK: Int,
    onTopKChange: (Int) -> Unit,
    maxTokens: Int,
    onMaxTokensChange: (Int) -> Unit,
    isGeneratingPrompt: Boolean,
    onAutoGeneratePrompt: () -> Unit,
    selectedCodeTab: Int,
    onCodeTabChange: (Int) -> Unit,
    onCopyCode: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = GeminiViolet)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PASO 4 — Construcción Guiada & Código", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // AI Studio UI vs Code Split Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📋 Dónde se hace cada parte:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• 🖥️ En AI Studio UI: Creas el Prompt, pegas el System Instruction, ajustas la Temperatura y pruebas en vivo.", fontSize = 12.sp)
                Text("• 💻 En tu Aplicación: Llamas a la API con la clave generada usando el SDK oficial o cURL.", fontSize = 12.sp)
            }
        }

        // System Instruction Editor with Auto-Generator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("System Instruction", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onAutoGeneratePrompt,
                        enabled = !isGeneratingPrompt,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isGeneratingPrompt) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generar con IA", fontSize = 11.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = onSystemPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            }
        }

        // Model & Hyperparameters Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚙️ Parámetros de Generación", fontWeight = FontWeight.Bold)

                Text("Modelo: $selectedModel", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GeminiModels.ALL.forEach { m ->
                        FilterChip(
                            selected = selectedModel == m.id,
                            onClick = { onModelChange(m.id) },
                            label = { Text(m.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperatura: %.2f".format(temperature), fontSize = 12.sp)
                        Text(if (temperature < 0.4f) "Muy precisa" else if (temperature > 1.0f) "Muy creativa" else "Equilibrada", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = temperature,
                        onValueChange = onTemperatureChange,
                        valueRange = 0.0f..2.0f
                    )
                }

                // Top-P Slider
                Column {
                    Text("Top-P: %.2f".format(topP), fontSize = 12.sp)
                    Slider(
                        value = topP,
                        onValueChange = onTopPChange,
                        valueRange = 0.0f..1.0f
                    )
                }
            }
        }

        // Exportable Code Snippets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CodeBgDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedCodeTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        listOf("Python", "Kotlin", "cURL", "JS").forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedCodeTab == index,
                                onClick = { onCodeTabChange(index) },
                                text = { Text(tabTitle, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                val currentCode = when (selectedCodeTab) {
                    0 -> """
# Python: Google GenAI SDK
from google import genai
from google.genai import types

client = genai.Client()

response = client.models.generate_content(
    model='$selectedModel',
    contents='Hola, ¿cómo estás?',
    config=types.GenerateContentConfig(
        system_instruction='''$systemPrompt''',
        temperature=$temperature,
        top_p=$topP,
        max_output_tokens=$maxTokens
    )
)
print(response.text)
                    """.trimIndent()
                    1 -> """
// Kotlin (Android Direct REST)
val request = GenerateContentRequest(
    contents = listOf(Content(parts = listOf(Part(text = "Hola")))),
    systemInstruction = Content(parts = listOf(Part(text = "${systemPrompt.take(80)}..."))),
    generationConfig = GenerationConfig(
        temperature = ${temperature}f,
        topP = ${topP}f,
        maxTokens = $maxTokens
    )
)
val response = RetrofitClient.service.generateContent(apiKey, request)
                    """.trimIndent()
                    2 -> """
curl "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent?key=${'$'}GEMINI_API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{
    "contents": [{"parts": [{"text": "Hola"}]}],
    "systemInstruction": {"parts": [{"text": "${systemPrompt.take(80)}..."}]},
    "generationConfig": {"temperature": $temperature, "topP": $topP}
  }'
                    """.trimIndent()
                    else -> """
// JavaScript / Web Fetch
const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent?key=${'$'}{API_KEY}`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    contents: [{ parts: [{ text: "Hola" }] }],
    systemInstruction: { parts: [{ text: "$systemPrompt" }] },
    generationConfig: { temperature: $temperature, topP: $topP }
  })
});
const data = await res.json();
console.log(data);
                    """.trimIndent()
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentCode,
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onCopyCode(currentCode) },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar código", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun Step5Pruebas(
    testInputText: String,
    onTestInputChange: (String) -> Unit,
    testResponseText: String,
    isTesting: Boolean,
    onRunTest: () -> Unit,
    onConsultMentor: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Science, contentDescription = null, tint = GeminiAmber)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PASO 5 — Pruebas & Ajustes en Vivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🧪 Sandbox de Evaluación", fontWeight = FontWeight.Bold)
                Text(
                    "Prueba cómo responde tu modelo configurado antes de desplegarlo en producción:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = testInputText,
                    onValueChange = onTestInputChange,
                    label = { Text("Mensaje de prueba del usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = onRunTest,
                    enabled = testInputText.isNotBlank() && !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ejecutando prueba...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ejecutar Prueba con Gemini")
                    }
                }

                if (testResponseText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Resultado del Modelo:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = testResponseText,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onConsultMentor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pedir a IA Student feedback de este resultado")
                    }
                }
            }
        }
    }
}
