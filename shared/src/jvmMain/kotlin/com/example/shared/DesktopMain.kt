package com.example.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.shared.ui.DesktopAssistantScreen

fun main() = application {
    val windowState = rememberWindowState(width = 1100.dp, height = 760.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "IA Student — Asistente de Voz y Reconocimiento STT",
        state = windowState
    ) {
        DesktopAssistantScreen()
    }
}

@Composable
fun DesktopApp() {
    DesktopAssistantScreen()
}

