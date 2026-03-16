package id.andriawan.cofinance.data.datasource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
actual fun compressImage(image: ByteArray, maxWidth: Int, quality: Int): ByteArray {
    val nsData = image.toNSData()
    val uiImage = UIImage.imageWithData(nsData) ?: return image

    val originalWidth = uiImage.size.useContents { width }
    val originalHeight = uiImage.size.useContents { height }

    val scaleFactor = if (originalWidth > maxWidth) {
        maxWidth.toDouble() / originalWidth
    } else {
        1.0
    }

    val resizedImage = if (scaleFactor < 1.0) {
        val newWidth = maxWidth.toDouble()
        val newHeight = originalHeight * scaleFactor
        val newSize = CGSizeMake(newWidth, newHeight)
        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        uiImage.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
        val scaled = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        scaled ?: uiImage
    } else {
        uiImage
    }

    val jpegData = UIImageJPEGRepresentation(resizedImage, quality / 100.0) ?: return image
    return jpegData.toByteArray()
}

actual fun saveTempImage(image: ByteArray): String {
    val tempDir = NSTemporaryDirectory()
    val fileName = "receipt_${NSDate().timeIntervalSince1970.toLong()}.jpg"
    val path = "$tempDir$fileName"
    val data = image.toNSData()
    data.writeToFile(path, atomically = true)
    return path
}

actual fun deleteTempFile(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    return bytes?.readBytes(length.toInt()) ?: ByteArray(0)
}
