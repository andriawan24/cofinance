package id.andriawan.cofinance.utils

import coil3.PlatformContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    return try {
        val file = File(URI(fileUri))
        FileInputStream(file).use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun deleteFile(fileUri: String) {
    try {
        File(URI(fileUri)).delete()
    } catch (_: Exception) {
        // Ignore deletion errors
    }
}

actual fun compressImage(imageBytes: ByteArray, maxDimension: Int, quality: Int): ByteArray {
    val original = ImageIO.read(ByteArrayInputStream(imageBytes)) ?: return imageBytes

    val scale = maxDimension.toFloat() / maxOf(original.width, original.height)
    val image = if (scale < 1f) {
        val newWidth = (original.width * scale).toInt()
        val newHeight = (original.height * scale).toInt()
        val scaled = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.drawImage(original, 0, 0, newWidth, newHeight, null)
        g.dispose()
        scaled
    } else {
        original
    }

    val output = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val param = writer.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = quality / 100f
    }
    writer.output = ImageIO.createImageOutputStream(output)
    writer.write(null, IIOImage(image, null, null), param)
    writer.dispose()
    return output.toByteArray()
}
