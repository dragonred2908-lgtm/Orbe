package com.example.shared.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.VoiceAssistantState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

/**
 * Carga el orbe personalizado del usuario (GIF animado o imagen estática).
 *
 * Ubicaciones admitidas (se usa la más reciente):
 *   1. ~/.ia-student/orb.gif | orb.png | orb.jpg | orb.jpeg  (sin recompilar)
 *   2. recursos empaquetados: orb_ia.gif | orb_ia.png
 *
 * Los GIF se decodifican con javax.imageio (sin dependencias extra) y se
 * reducen a un tamaño máximo razonable para que el renderizado por software
 * en PCs antiguas no se sature.
 */
object OrbImageLoader {

    data class OrbFrame(val image: BufferedImage, val delayMs: Int)

    private const val MAX_DIMENSION = 720
    private val CANDIDATE_NAMES = listOf("orb.gif", "orb.png", "orb.jpg", "orb.jpeg")

    fun orbDir(): File = File(System.getProperty("user.home"), ".ia-student")

    /** Archivo de orbe del usuario más reciente, si existe. */
    fun findUserOrbFile(): File? {
        return CANDIDATE_NAMES
            .map { File(orbDir(), it) }
            .filter { it.isFile && it.length() > 0 }
            .maxByOrNull { it.lastModified() }
    }

    /** Archivo empaquetado en recursos (si el usuario no aportó uno). */
    private fun findBundledOrbStream(): Pair<String, () -> java.io.InputStream>? {
        for (name in listOf("orb_ia.gif", "orb_ia.png")) {
            val stream = OrbImageLoader::class.java.getResourceAsStream("/$name")
            if (stream != null) {
                stream.close()
                return name to { OrbImageLoader::class.java.getResourceAsStream("/$name")!! }
            }
        }
        return null
    }

    /** Carga fotogramas desde disco (GIF animado o imagen única). */
    fun loadFrames(file: File): List<OrbFrame> {
        if (file.extension.lowercase() == "gif") {
            return loadGif(file)
        }
        val image = ImageIO.read(file) ?: return emptyList()
        return listOf(OrbFrame(scaleDown(image), 0))
    }

    /** Carga fotogramas empaquetados en el classpath. */
    fun loadBundledFrames(): List<OrbFrame> {
        val bundled = findBundledOrbStream() ?: return emptyList()
        val (name, opener) = bundled
        return if (name.endsWith(".gif", ignoreCase = true)) {
            loadGifStream(opener())
        } else {
            opener().use { stream ->
                ImageIO.read(stream)?.let { listOf(OrbFrame(scaleDown(it), 0)) } ?: emptyList()
            }
        }
    }

    private fun loadGif(file: File): List<OrbFrame> {
        return try {
            loadGifStream(file.inputStream())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadGifStream(input: java.io.InputStream): List<OrbFrame> {
        val frames = mutableListOf<OrbFrame>()
        var iis: ImageInputStream? = null
        try {
            iis = ImageIO.createImageInputStream(input)
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            reader.input = iis
            val count = runCatching { reader.getNumImages(true) }.getOrDefault(0)

            if (count <= 0) {
                val single = ImageIO.read(input)
                return if (single != null) listOf(OrbFrame(scaleDown(single), 0)) else emptyList()
            }

            // Lienzo acumulativo: los GIF pueden usar fotogramas parciales.
            var canvas: BufferedImage? = null

            for (index in 0 until count) {
                val frame = runCatching { reader.read(index) }.getOrNull() ?: continue
                val delay = readGifDelay(reader, index)

                if (canvas == null || canvas!!.width != frame.width || canvas!!.height != frame.height) {
                    canvas = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB)
                }
                val g = canvas!!.createGraphics()
                g.drawImage(frame, 0, 0, null)
                g.dispose()

                val snapshot = BufferedImage(canvas!!.width, canvas!!.height, BufferedImage.TYPE_INT_ARGB)
                val g2 = snapshot.createGraphics()
                g2.drawImage(canvas, 0, 0, null)
                g2.dispose()

                frames.add(OrbFrame(scaleDown(snapshot), delay))
            }
            reader.dispose()
        } catch (e: Exception) {
            // Devolver lo que se haya podido leer.
        } finally {
            runCatching { iis?.close() }
        }
        return frames
    }

    /** Lee el retardo del fotograma (en centésimas de segundo según el GIF). */
    private fun readGifDelay(reader: javax.imageio.ImageReader, index: Int): Int {
        return try {
            val meta = reader.getImageMetadata(index)
            val root = meta.getAsTree("javax_imageio_gif_image_1.0") as? org.w3c.dom.Element
                ?: return 8
            val nodes = root.getElementsByTagName("GraphicControlExtension")
            if (nodes.length == 0) return 8
            val element = nodes.item(0) as? org.w3c.dom.Element ?: return 8
            val delayHundredths = element.getAttribute("delayTime").toIntOrNull() ?: 8
            // delayTime viene en centésimas de segundo
            (delayHundredths * 10).coerceIn(30, 250)
        } catch (e: Exception) {
            8 * 10
        }
    }

    private fun scaleDown(image: BufferedImage): BufferedImage {
        val maxSide = maxOf(image.width, image.height)
        if (maxSide <= MAX_DIMENSION) return image
        val scale = MAX_DIMENSION.toDouble() / maxSide
        val w = (image.width * scale).toInt().coerceAtLeast(1)
        val h = (image.height * scale).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g.drawImage(image, 0, 0, w, h, null)
        g.dispose()
        return scaled
    }

    /**
     * Abre el diálogo nativo para elegir una imagen y la copia a
     * ~/.ia-student/orb.<ext>. Invoca [onPicked] desde el hilo de UI al terminar.
     */
    fun pickAndInstallOrb(onPicked: () -> Unit) {
        Thread({
            try {
                val dialog = FileDialog(null as Frame?, "Elige tu orbe (GIF animado, PNG o JPG)", FileDialog.LOAD)
                dialog.setFilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".gif") || lower.endsWith(".png") ||
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                }
                dialog.isVisible = true
                val selectedName = dialog.file
                if (!selectedName.isNullOrBlank()) {
                    val source = File(dialog.directory ?: "", selectedName)
                    if (source.isFile) {
                        val ext = selectedName.substringAfterLast('.', "gif").lowercase()
                        // Eliminar orbes anteriores para que no gane el más antiguo
                        CANDIDATE_NAMES.forEach { File(orbDir(), it).delete() }
                        orbDir().mkdirs()
                        source.copyTo(File(orbDir(), "orb.$ext"), overwrite = true)
                        javax.swing.SwingUtilities.invokeLater { onPicked() }
                    }
                }
            } catch (_: Exception) {
                // Cancelado o error del diálogo: no hacer nada.
            }
        }, "OrbPicker").apply { isDaemon = true }.start()
    }
}

/** Fotograma ya convertido a bitmap de Compose (sin duplicar la imagen original). */
private class OrbAnimationFrame(val bitmap: ImageBitmap, val delayMs: Int)

/**
 * Orbe principal a pantalla completa:
 *  - Si el usuario aportó una imagen/GIF, la reproduce animada con un glow sutil.
 *  - Si no, usa el orbe procedural cuántico como respaldo y muestra una pista.
 */
@Composable
fun OrbHero(
    stateFlow: kotlinx.coroutines.flow.StateFlow<VoiceAssistantState>,
    rmsFlow: kotlinx.coroutines.flow.StateFlow<Float>,
    modifier: Modifier = Modifier,
    reloadKey: Int = 0
) {
    var frames by remember { mutableStateOf<List<OrbAnimationFrame>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(reloadKey) {
        loading = true
        frames = withContext(Dispatchers.IO) {
            val raw = runCatching {
                val userFile = OrbImageLoader.findUserOrbFile()
                if (userFile != null) {
                    OrbImageLoader.loadFrames(userFile)
                } else {
                    OrbImageLoader.loadBundledFrames()
                }
            }.getOrDefault(emptyList())
            prepareFrames(raw)
        }
        loading = false
    }

    if (frames.isNotEmpty()) {
        AnimatedOrb(frames = frames, modifier = modifier)
    } else {
        val state by stateFlow.collectAsState()
        val rms by rmsFlow.collectAsState()
        ProceduralOrbFallback(state = state, rms = rms, showHint = !loading, modifier = modifier)
    }
}

/**
 * Convierte los fotogramas a bitmaps de Compose una sola vez y limita el número
 * de fotogramas para que la RAM no se dispare en equipos antiguos.
 *
 * Si el GIF trae muchísimos fotogramas se toman uno de cada N y se multiplica
 * su retardo, conservando la duración total del bucle.
 */
private fun prepareFrames(raw: List<OrbImageLoader.OrbFrame>): List<OrbAnimationFrame> {
    if (raw.isEmpty()) return emptyList()

    val maxFrames = 72
    val step = if (raw.size > maxFrames) {
        (raw.size + maxFrames - 1) / maxFrames
    } else {
        1
    }

    return raw.asSequence()
        .filterIndexed { index, _ -> index % step == 0 }
        .take(maxFrames)
        .map { frame ->
            OrbAnimationFrame(
                bitmap = frame.image.toComposeImageBitmap(),
                delayMs = (frame.delayMs * step).coerceAtLeast(40)
            )
        }
        .toList()
}

/** Reproduce un GIF (o imagen fija) centrado a pantalla completa con glow. */
@Composable
private fun AnimatedOrb(frames: List<OrbAnimationFrame>, modifier: Modifier = Modifier) {
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames) {
        if (frames.size > 1) {
            while (true) {
                val frameDelay = frames[index.coerceIn(0, frames.lastIndex)].delayMs.coerceAtLeast(40)
                delay(frameDelay.toLong())
                index = (index + 1) % frames.size
            }
        }
    }

    val current: ImageBitmap = frames[index.coerceIn(0, frames.lastIndex)].bitmap

    // Pulso lento y barato (sin recalcular el orbe): escala 1.00 ⇄ 1.03
    val pulse by rememberInfiniteTransition(label = "orbPulse").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulseScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Glow radial tenue detrás del orbe (muy barato: un gradiente estático)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00C8D8).copy(alpha = 0.10f),
                            Color(0xFF001018).copy(alpha = 0.0f)
                        )
                    )
                )
        )
        Image(
            bitmap = current,
            contentDescription = "Orbe del asistente",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .padding(12.dp),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Low
        )
    }
}

/** Respaldo con el orbe procedural (si el usuario no ha aportado su imagen). */
@Composable
private fun ProceduralOrbFallback(
    state: VoiceAssistantState,
    rms: Float,
    showHint: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            DesktopVoiceVisualizer(
                state = state,
                rmsLevel = rms,
                modifier = Modifier.size(340.dp)
            )
        }

        if (showHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x590B1220))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🖼️ Menú ☰ → «Elegir imagen del orbe…»  ·  o guarda tu GIF en ~/.ia-student/orb.gif",
                    color = Color(0xFF7DD3FC),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
