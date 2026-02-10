package id.andriawan.cofinance.utils

import coil3.PlatformContext
import java.io.File
import java.io.FileInputStream
import java.net.URI

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    return try {
        val file = File(URI(fileUri))
        FileInputStream(file).use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
