package id.andriawan.cofinance.utils

import androidx.core.net.toUri
import coil3.PlatformContext
import java.io.File

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    val uri = fileUri.toUri()

    if (uri.scheme == null || uri.scheme == "file") {
        val path = uri.path ?: return null
        return File(path).takeIf { it.exists() }?.readBytes()
    }

    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}
