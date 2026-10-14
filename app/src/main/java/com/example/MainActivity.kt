package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import com.example.data.repository.ProjectRepository
import com.example.ui.chat.ChatScreen
import com.example.ui.hub.KnowledgeHubScreen
import com.example.ui.multimodal.MultimodalLabScreen
import com.example.ui.orb.JarvisOrbScreen
import com.example.ui.projects.ProjectsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.wizard.StepWizardScreen

sealed class Screen(val route: String, val title: String) {
    object JarvisOrb : Screen("orb", "Orbe Cian")
    object Chat : Screen("chat", "Chat IA Student")
    object Wizard : Screen("wizard", "Paso a Paso")
    object Multimodal : Screen("multimodal", "Studio Lab")
    object Knowledge : Screen("knowledge", "Centro de Conocimiento")
    object Projects : Screen("projects", "Mis Proyectos")
}

class MainActivity : ComponentActivity() {

    private val apiClient by lazy { GeminiApiClient() }
    private val audioPlayer by lazy { AudioPlayerHelper(this) }
    private val projectRepository by lazy { ProjectRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold(
                    apiClient = apiClient,
                    audioPlayer = audioPlayer,
                    projectRepository = projectRepository
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    projectRepository: ProjectRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.JarvisOrb) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }

    // Al presionar Atrás desde cualquier pantalla secundaria, regresa inmediatamente al Orbe Puro
    BackHandler(enabled = currentScreen != Screen.JarvisOrb) {
        currentScreen = Screen.JarvisOrb
    }

    when (currentScreen) {
        // --- 1. PANTALLA PRINCIPAL: ÚNICAMENTE EL ORBE CIAN EN PANTALLA COMPLETA ---
        Screen.JarvisOrb -> {
            JarvisOrbScreen(
                apiClient = apiClient,
                audioPlayer = audioPlayer,
                onNavigateTo = { destination ->
                    when (destination) {
                        "chat" -> currentScreen = Screen.Chat
                        "wizard" -> currentScreen = Screen.Wizard
                        "multimodal" -> currentScreen = Screen.Multimodal
                        "knowledge" -> currentScreen = Screen.Knowledge
                        "projects" -> currentScreen = Screen.Projects
                        else -> currentScreen = Screen.JarvisOrb
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. PANTALLAS SECUNDARIAS (Con botón volver al Orbe) ---
        else -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text(currentScreen.title) },
                        navigationIcon = {
                            IconButton(
                                onClick = { currentScreen = Screen.JarvisOrb },
                                modifier = Modifier.testTag("back_to_orb_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Volver al Orbe"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        Screen.Chat -> {
                            ChatScreen(
                                apiClient = apiClient,
                                audioPlayer = audioPlayer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.Wizard -> {
                            StepWizardScreen(
                                apiClient = apiClient,
                                projectRepository = projectRepository,
                                onNavigateToChatWithPrompt = { prompt ->
                                    initialChatPrompt = prompt
                                    currentScreen = Screen.Chat
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.Multimodal -> {
                            MultimodalLabScreen(
                                apiClient = apiClient,
                                audioPlayer = audioPlayer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.Knowledge -> {
                            KnowledgeHubScreen(
                                onSendQueryToChat = { query ->
                                    initialChatPrompt = query
                                    currentScreen = Screen.Chat
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.Projects -> {
                            ProjectsScreen(
                                projectRepository = projectRepository,
                                onOpenInChat = { msg ->
                                    initialChatPrompt = msg
                                    currentScreen = Screen.Chat
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
