package com.example.ui.orb

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

/**
 * Estados del Orbe Cuántico
 */
enum class OrbState {
    REPOSO,         // Ondas fluidas orgánicas y respiración cuántica calmada
    ESCUCHA,        // Ondas que se expanden hacia el exterior resonando con la voz
    PROCESAMIENTO   // Aceleración de túnel digital de alta frecuencia
}

private data class QuantumParticle(
    val ringIndex: Int,
    val angleRad: Float,
    val radiusSize: Float,
    val baseAlpha: Float,
    val speedOffset: Float
)

@Composable
fun CyanOrbCanvas(
    state: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = 320.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "QuantumWaveTransition")

    // Fase de onda continua (Ondulación fluida y orgánica)
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.REPOSO -> 6000
                    OrbState.ESCUCHA -> 3200
                    OrbState.PROCESAMIENTO -> 1400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Rotación suave del túnel digital
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.REPOSO -> 14000
                    OrbState.ESCUCHA -> 7000
                    OrbState.PROCESAMIENTO -> 2800
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "TunnelRotation"
    )

    // Pulsación cuántica de profundidad
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = when (state) {
            OrbState.REPOSO -> 0.96f
            OrbState.ESCUCHA -> 1.04f
            OrbState.PROCESAMIENTO -> 0.94f
        },
        targetValue = when (state) {
            OrbState.REPOSO -> 1.03f
            OrbState.ESCUCHA -> 1.15f
            OrbState.PROCESAMIENTO -> 1.08f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.REPOSO -> 2600
                    OrbState.ESCUCHA -> 1200
                    OrbState.PROCESAMIENTO -> 700
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // Generar enjambre de partículas en anillos concéntricos
    val particles = remember {
        val list = mutableListOf<QuantumParticle>()
        val rnd = Random(1337)
        for (ring in 0 until 6) {
            val countInRing = 20 + ring * 14
            for (i in 0 until countInRing) {
                val angle = (2f * Math.PI.toFloat() * i / countInRing.toFloat()) + rnd.nextFloat() * 0.15f
                list.add(
                    QuantumParticle(
                        ringIndex = ring,
                        angleRad = angle,
                        radiusSize = 1.2f + rnd.nextFloat() * 2.6f,
                        baseAlpha = 0.35f + rnd.nextFloat() * 0.65f,
                        speedOffset = 0.8f + rnd.nextFloat() * 0.4f
                    )
                )
            }
        }
        list
    }

    val colorCyanBright = Color(0xFF00F0FF)
    val colorTealLight = Color(0xFF00E5FF)
    val colorTealMedium = Color(0xFF00B4D8)
    val colorTealDeep = Color(0xFF0077B6)
    val colorCenterWhite = Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("quantum_cyan_orb_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = this.size.width
            val canvasH = this.size.height
            val center = Offset(canvasW / 2f, canvasH / 2f)
            val maxRadius = (min(canvasW, canvasH) / 2f) * breathingScale

            // 1. Aura cósmica exterior y túnel de profundidad
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3800F0FF),
                        Color(0x1F00B4D8),
                        Color(0x0A0077B6),
                        Color(0x00050B0E)
                    ),
                    center = center,
                    radius = maxRadius * 1.15f
                ),
                radius = maxRadius * 1.15f,
                center = center
            )

            // 2. Anillos concéntricos ondulantes (Estructuras en forma de onda fluidas y orgánicas)
            val numRings = 7
            rotate(degrees = rotationDegrees * 0.4f, pivot = center) {
                for (rIdx in 1..numRings) {
                    val ringRatio = rIdx.toFloat() / numRings.toFloat()
                    val baseR = maxRadius * ringRatio * 0.88f
                    val waveHarmonic = 5 + (rIdx % 3)
                    val waveAmplitude = (4f + rIdx * 2.2f) * when (state) {
                        OrbState.REPOSO -> 1.0f
                        OrbState.ESCUCHA -> 1.6f
                        OrbState.PROCESAMIENTO -> 1.3f
                    }

                    val path = Path()
                    val steps = 120
                    for (s in 0..steps) {
                        val angle = (2f * Math.PI.toFloat() * s.toFloat() / steps.toFloat())
                        val direction = if (rIdx % 2 == 0) 1f else -1f
                        val ripple = sin((angle * waveHarmonic + wavePhase * direction + rIdx.toFloat()).toDouble()).toFloat() * waveAmplitude
                        val finalR = baseR + ripple
                        val px = center.x + finalR * cos(angle.toDouble()).toFloat()
                        val py = center.y + finalR * sin(angle.toDouble()).toFloat()

                        if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()

                    val ringColor = when (rIdx) {
                        1 -> colorCyanBright.copy(alpha = 0.75f)
                        2, 3 -> colorTealLight.copy(alpha = 0.55f)
                        4, 5 -> colorTealMedium.copy(alpha = 0.40f)
                        else -> colorTealDeep.copy(alpha = 0.25f)
                    }

                    drawPath(
                        path = path,
                        color = ringColor,
                        style = Stroke(
                            width = if (rIdx <= 2) 1.6f else 1.2f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // 3. Partículas de luz brillante interconectadas sobre las ondas
            rotate(degrees = -rotationDegrees * 0.6f, pivot = center) {
                var prevPoint: Offset? = null
                var prevRing = -1

                for (p in particles) {
                    val ringRatio = (p.ringIndex + 1).toFloat() / 6f
                    val baseR = maxRadius * ringRatio * 0.85f
                    val waveHarmonic = 5 + (p.ringIndex % 3)
                    val waveAmp = (4f + p.ringIndex.toFloat() * 2f)

                    val dynamicAngle = (p.angleRad + wavePhase * 0.15f * p.speedOffset)
                    val ripple = sin((dynamicAngle * waveHarmonic + wavePhase).toDouble()).toFloat() * waveAmp
                    val finalR = baseR + ripple

                    val px = center.x + finalR * cos(dynamicAngle.toDouble()).toFloat()
                    val py = center.y + finalR * sin(dynamicAngle.toDouble()).toFloat()
                    val currentPoint = Offset(px, py)

                    if (prevPoint != null && prevRing == p.ringIndex) {
                        val dx = currentPoint.x - prevPoint.x
                        val dy = currentPoint.y - prevPoint.y
                        val distSq = dx * dx + dy * dy
                        val threshold = (maxRadius * 0.35f) * (maxRadius * 0.35f)
                        if (distSq < threshold) {
                            drawLine(
                                color = colorTealLight.copy(alpha = 0.18f),
                                start = prevPoint,
                                end = currentPoint,
                                strokeWidth = 1.0f
                            )
                        }
                    }
                    prevPoint = currentPoint
                    prevRing = p.ringIndex

                    val particleColor = when {
                        p.radiusSize > 3.0f -> colorCenterWhite.copy(alpha = min(1.0f, p.baseAlpha + 0.25f))
                        p.radiusSize > 2.0f -> colorCyanBright.copy(alpha = p.baseAlpha)
                        else -> colorTealMedium.copy(alpha = p.baseAlpha * 0.9f)
                    }

                    drawCircle(
                        color = particleColor,
                        radius = p.radiusSize * (if (state == OrbState.ESCUCHA) 1.25f else 1.0f),
                        center = currentPoint
                    )
                }
            }

            // 4. Centro exacto: Pequeño y sutil núcleo esférico brillante (Punto Focal)
            val subtleCoreRadius = maxRadius * 0.085f
            val coreGlowRadius = maxRadius * 0.18f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorCyanBright.copy(alpha = 0.70f),
                        colorTealLight.copy(alpha = 0.35f),
                        Color(0x00050B0E)
                    ),
                    center = center,
                    radius = coreGlowRadius
                ),
                radius = coreGlowRadius,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorCenterWhite,
                        colorCyanBright,
                        colorTealMedium.copy(alpha = 0.8f)
                    ),
                    center = center,
                    radius = subtleCoreRadius
                ),
                radius = subtleCoreRadius,
                center = center
            )

            drawCircle(
                color = colorCenterWhite.copy(alpha = 0.9f),
                radius = subtleCoreRadius * 0.45f,
                center = center
            )
        }
    }
}
