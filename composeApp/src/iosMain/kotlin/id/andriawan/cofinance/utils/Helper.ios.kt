package id.andriawan.cofinance.utils

import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    val nsUrl = NSURL.URLWithString(fileUri) ?: return null

    val accessed = nsUrl.startAccessingSecurityScopedResource()

    return try {
        val data = NSData.dataWithContentsOfURL(nsUrl) ?: return null
        data.toByteArray()
    } finally {
        if (accessed) {
            nsUrl.stopAccessingSecurityScopedResource()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}
