package id.andriawan.cofinance.utils

import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSMutableData
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual fun ByteArray.toFirebaseData(): Data {
    val data = NSMutableData().apply { setLength(size.toULong()) }
    usePinned { pinned ->
        memcpy(data.mutableBytes, pinned.addressOf(0), size.toULong())
    }
    return Data(data)
}
