package com.example.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.shared.ui.DesktopAssistantScreen

/**
 * Punto de entrada principal para Compose Desktop (Windows, macOS, Linux).
 * Proporciona compatibilidad completa con llamadas de clase (DesktopMain) y clase generada (DesktopMainKt).
 */

fun main(args: Array<String>) = runDesktopApplication()

fun main() = runDesktopApplication()

/**
 * Clase contenedora explícita para garantizar resolución por reflexión
 * y evitar errores NoClassDefFoundError en launchers de JVM.
 */
class DesktopMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runDesktopApplication()
        }
    }
}

fun runDesktopApplication() = application {
    val windowState = rememberWindowState(width = 1100.dp, height = 760.dp)

    // Carga resiliente del ícono del orbe cuántico desde classpath de JVM
    val appIcon: Painter? = try {
        useResource("icon.png") { inputStream ->
            BitmapPainter(loadImageBitmap(inputStream))
        }
    } catch (_: Exception) {
        null
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "IA Student — Asistente de Voz y Reconocimiento STT",
        state = windowState,
        icon = appIcon
    ) {
        DesktopAssistantScreen()
    }
}

@Composable
fun DesktopApp() {
    DesktopAssistantScreen()
}
