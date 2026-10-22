package com.example.shared.ui

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.stream.ImageOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica que el cargador de orbes sepa leer GIF animados y PNG estáticos,
 * que es lo que alimenta el orbe a pantalla completa del usuario.
 */
class OrbImageLoaderTest {

    private fun tempFile(extension: String): File {
        val file = File.createTempFile("orbe-test-", ".$extension")
        file.deleteOnExit()
        return file
    }

    private fun frameColor(index: Int): Color = when (index % 3) {
        0 -> Color(0x00, 0xC8, 0xD8)
        1 -> Color(0x70, 0x00, 0xFF)
        else -> Color(0x00, 0xE6, 0x76)
    }

    private fun writeGif(file: File, frames: Int) {
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        val stream: ImageOutputStream = ImageIO.createImageOutputStream(file)
        writer.output = stream
        writer.prepareWriteSequence(null)
        for (index in 0 until frames) {
            val image = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            graphics.color = frameColor(index)
            graphics.fillRect(0, 0, 24, 24)
            graphics.dispose()
            val specifier = ImageTypeSpecifier.createFromRenderedImage(image)
            val metadata = writer.getDefaultImageMetadata(specifier, writer.defaultWriteParam)
            writer.writeToSequence(IIOImage(image, null, metadata), writer.defaultWriteParam)
        }
        writer.endWriteSequence()
        stream.close()
        writer.dispose()
    }

    @Test
    fun testCargaGifAnimadoConVariosFotogramas() {
        val file = tempFile("gif")
        writeGif(file, frames = 4)

        val loaded = OrbImageLoader.loadFrames(file)

        assertTrue(loaded.size >= 2, "El GIF animado debe producir varios fotogramas, obtuvo ${loaded.size}")
        loaded.forEach { frame ->
            assertTrue(frame.image.width > 0 && frame.image.height > 0, "Cada fotograma debe tener imagen válida")
            assertTrue(frame.delayMs >= 30, "El retardo debe ser razonable: ${frame.delayMs}")
        }
    }

    @Test
    fun testCargaPngEstatico() {
        val file = tempFile("png")
        val image = BufferedImage(32, 48, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(0x00, 0xC8, 0xD8)
        graphics.fillRect(0, 0, 32, 48)
        graphics.dispose()
        ImageIO.write(image, "png", file)

        val loaded = OrbImageLoader.loadFrames(file)

        assertEquals(1, loaded.size, "Un PNG debe producir un único fotograma")
        assertEquals(32, loaded.first().image.width)
        assertEquals(48, loaded.first().image.height)
    }

    @Test
    fun testArchivoInexistenteNoCrashea() {
        // loadFrames con un archivo inválido no debe lanzar excepción hacia la UI
        val file = File("no-existe-orb.gif")
        val loaded = OrbImageLoader.loadFrames(file)
        assertTrue(loaded.isEmpty(), "Un archivo inexistente debe devolver lista vacía")
    }

    @Test
    fun testCarpetaDeOrbesEsDotIaStudent() {
        val dir = OrbImageLoader.orbDir()
        assertEquals(".ia-student", dir.name)
        assertEquals(System.getProperty("user.home"), dir.parentFile?.path)
    }
}
