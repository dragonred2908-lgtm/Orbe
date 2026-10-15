package com.example.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.shared.model.VoiceAssistantState

/**
 * Orbe visualizador cuántico de voz reactivo a la amplitud de audio (RMS) y estado de la sesión.
 */
@Composable
fun DesktopVoiceVisualizer(
    state: VoiceAssistantState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Animación suave del nivel de audio
    val smoothRms = remember { Animatable(0f) }
    LaunchedEffect(rmsLevel) {
        smoothRms.animateTo(
            targetValue = rmsLevel,
            animationSpec = tween(80, easing = LinearEasing)
        )
    }

    // Colores según el estado del asistente
    val (primaryColor, glowColor) = when (state) {
        is VoiceAssistantState.Idle -> Pair(Color(0xFF00E5FF), Color(0xFF005577))
        is VoiceAssistantState.ListeningForWakePhrase -> Pair(Color(0xFF00F0FF), Color(0xFF007799))
        is VoiceAssistantState.WakePhraseDetected -> Pair(Color(0xFFFFD700), Color(0xFF997A00))
        is VoiceAssistantState.RecordingQuery -> Pair(Color(0xFFFF0055), Color(0xFF880022))
        is VoiceAssistantState.TranscribingSpeech -> Pair(Color(0xFFB026FF), Color(0xFF550088))
        is VoiceAssistantState.ProcessingWithAI -> Pair(Color(0xFF7000FF), Color(0xFF330077))
        is VoiceAssistantState.Responding -> Pair(Color(0xFF00E676), Color(0xFF006633))
        is VoiceAssistantState.Error -> Pair(Color(0xFFFF3366), Color(0xFF881122))
    }

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 3.4f) * pulseScale
            val dynamicRadius = baseRadius + (smoothRms.value * 28f)

            // Resplandor exterior (Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f + (smoothRms.value * 0.4f)),
                        glowColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.8f
                ),
                radius = dynamicRadius * 1.8f,
                center = center
            )

            // Anillos orbitales concéntricos
            val rings = 3
            for (i in 0 until rings) {
                val ringRadius = dynamicRadius * (0.85f + (i * 0.22f))
                val strokeWidth = (2.2f + (i * 0.6f) + (smoothRms.value * 3f))
                drawCircle(
                    color = primaryColor.copy(alpha = 0.5f - (i * 0.12f)),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }

            // Núcleo cuántico central
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        glowColor
                    ),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )
        }
    }
}
