package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import com.example.data.repository.AssistantSettingsRepository
import com.example.data.repository.BusinessAssistantRepository
import com.example.data.repository.CrossPlatformRepository
import com.example.data.repository.FileSearchRepository
import com.example.data.repository.LegalConsentRepository
import com.example.data.repository.PermissionsRepository
import com.example.data.repository.ProjectRepository
import com.example.ui.business.BusinessAssistantScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.desktop.DesktopInstallScreen
import com.example.ui.files.FileSearchScreen
import com.example.ui.hub.KnowledgeHubScreen
import com.example.ui.legal.OnboardingConsentScreen
import com.example.ui.multimodal.MultimodalLabScreen
import com.example.ui.orb.QuantumOrbScreen
import com.example.ui.permissions.PermissionsControlScreen
import com.example.ui.projects.ProjectsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.wizard.StepWizardScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String) {
    object QuantumOrb : Screen("orb", "Orbe Principal")
    object Chat : Screen("chat", "Chat & Búsqueda Web")
    object DesktopInstall : Screen("desktop", "Instalación en PC & Multiplataforma")
    object FileSearch : Screen("files", "Buscador de Archivos (Móvil & PC)")
    object BusinessAssistant : Screen("business", "Asistente Comercial (Auto-Responder)")
    object Permissions : Screen("permissions", "Permisos & Control de Usos")
    object Multimodal : Screen("multimodal", "Crear y Editar Fotos / Videos")
    object Wizard : Screen("wizard", "Paso a Paso")
    object Knowledge : Screen("knowledge", "Centro de Conocimiento")
    object Projects : Screen("projects", "Mis Proyectos")
}

class MainActivity : ComponentActivity() {

    private val apiClient by lazy { GeminiApiClient() }
    private val audioPlayer by lazy { AudioPlayerHelper(this) }
    private val projectRepository by lazy { ProjectRepository(this) }
    private val settingsRepository by lazy { AssistantSettingsRepository(this) }
    private val crossPlatformRepo by lazy { CrossPlatformRepository(this) }
    private val fileSearchRepo by lazy { FileSearchRepository(this) }
    private val businessRepo by lazy { BusinessAssistantRepository(this) }
    private val permissionsRepo by lazy { PermissionsRepository(this) }
    private val consentRepo by lazy { LegalConsentRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()
                val consentState by consentRepo.consentStateFlow.collectAsState(initial = null)

                if (consentState?.isAccepted != true) {
                    OnboardingConsentScreen(
                        onAcceptConsent = {
                            coroutineScope.launch {
                                consentRepo.recordConsent(true)
                            }
                        },
                        onDeclineConsent = {
                            finish()
                        }
                    )
                } else {
                    MainAppScaffold(
                        apiClient = apiClient,
                        audioPlayer = audioPlayer,
                        projectRepository = projectRepository,
                        settingsRepository = settingsRepository,
                        crossPlatformRepo = crossPlatformRepo,
                        fileSearchRepo = fileSearchRepo,
                        businessRepo = businessRepo,
                        permissionsRepo = permissionsRepo
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    projectRepository: ProjectRepository,
    settingsRepository: AssistantSettingsRepository,
    crossPlatformRepo: CrossPlatformRepository,
    fileSearchRepo: FileSearchRepository,
    businessRepo: BusinessAssistantRepository,
    permissionsRepo: PermissionsRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.QuantumOrb) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }

    // Al presionar Atrás desde cualquier pantalla secundaria, regresa inmediatamente al Orbe Puro
    BackHandler(enabled = currentScreen != Screen.QuantumOrb) {
        currentScreen = Screen.QuantumOrb
    }

    when (currentScreen) {
        // --- 1. PANTALLA PRINCIPAL: ÚNICAMENTE EL ORBE CIAN EN PANTALLA COMPLETA ---
        Screen.QuantumOrb -> {
            QuantumOrbScreen(
                apiClient = apiClient,
                audioPlayer = audioPlayer,
                settingsRepository = settingsRepository,
                onNavigateTo = { destination ->
                    when (destination) {
                        "chat" -> currentScreen = Screen.Chat
                        "desktop" -> currentScreen = Screen.DesktopInstall
                        "files" -> currentScreen = Screen.FileSearch
                        "business" -> currentScreen = Screen.BusinessAssistant
                        "permissions" -> currentScreen = Screen.Permissions
                        "multimodal" -> currentScreen = Screen.Multimodal
                        "wizard" -> currentScreen = Screen.Wizard
                        "knowledge" -> currentScreen = Screen.Knowledge
                        "projects" -> currentScreen = Screen.Projects
                        else -> currentScreen = Screen.QuantumOrb
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
                                onClick = { currentScreen = Screen.QuantumOrb },
                                modifier = Modifier.testTag("back_to_orb_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        Screen.DesktopInstall -> {
                            DesktopInstallScreen(
                                crossPlatformRepo = crossPlatformRepo,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.FileSearch -> {
                            FileSearchScreen(
                                fileSearchRepo = fileSearchRepo,
                                crossPlatformRepo = crossPlatformRepo,
                                onConsultAssistantWithFile = { prompt ->
                                    initialChatPrompt = prompt
                                    currentScreen = Screen.Chat
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.BusinessAssistant -> {
                            BusinessAssistantScreen(
                                businessRepo = businessRepo,
                                apiClient = apiClient,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.Permissions -> {
                            PermissionsControlScreen(
                                permissionsRepo = permissionsRepo,
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
