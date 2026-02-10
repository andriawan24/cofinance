package id.andriawan.cofinance.utils

import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.dataWithContentsOfURL
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsVersionCurrent
import platform.posix.memcpy

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    if (fileUri.startsWith("ph://")) {
        return readFromPhotosLibrary(fileUri)
    }

    val nsUrl = NSURL.URLWithString(fileUri)
        ?: NSURL.fileURLWithPath(fileUri)

    val accessed = nsUrl.startAccessingSecurityScopedResource()

    return try {
        val data = NSData.dataWithContentsOfFile(fileUri) ?: return null
        data.toByteArray()
    } finally {
        if (accessed) {
            nsUrl.stopAccessingSecurityScopedResource()
        }
    }
}

private fun readFromPhotosLibrary(phUri: String): ByteArray? {
    val localIdentifier = phUri.removePrefix("ph://")

    val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(
        listOf(localIdentifier),
        null
    )

    val asset = fetchResult.firstObject as? PHAsset ?: return null

    val options = PHImageRequestOptions().apply {
        synchronous = true
        networkAccessAllowed = true
        version = PHImageRequestOptionsVersionCurrent
    }

    var resultData: NSData? = null

    PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(
        asset,
        options
    ) { data, _, _, _ ->
        resultData = data
    }

    return resultData?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFile(fileUri: String) {
    if (fileUri.startsWith("ph://")) return

    val nsUrl = NSURL.URLWithString(fileUri)
        ?: NSURL.fileURLWithPath(fileUri)
    val path = nsUrl.path ?: return
    NSFileManager.defaultManager.removeItemAtPath(path, null)
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
