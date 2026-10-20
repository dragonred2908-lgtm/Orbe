package com.example.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.shared.ui.DesktopAssistantScreen

fun main() = application {
    val windowState = rememberWindowState(width = 1100.dp, height = 760.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "IA Student — Asistente de Voz y Reconocimiento STT",
        state = windowState,
        icon = painterResource("icon.png")
    ) {
        DesktopAssistantScreen()
    }
}

@Composable
fun DesktopApp() {
    DesktopAssistantScreen()
}
