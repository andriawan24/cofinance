package id.andriawan.cofinance.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import coil3.PlatformContext
import java.io.ByteArrayOutputStream
import java.io.File

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    val uri = fileUri.toUri()

    if (uri.scheme == null || uri.scheme == "file") {
        val path = uri.path ?: return null
        return File(path).takeIf { it.exists() }?.readBytes()
    }

    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}

actual fun deleteFile(fileUri: String) {
    val uri = fileUri.toUri()
    val path = uri.path ?: return
    File(path).delete()
}

actual fun compressImage(imageBytes: ByteArray, maxDimension: Int, quality: Int): ByteArray {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    val width = options.outWidth
    val height = options.outHeight
    if (width <= 0 || height <= 0) return imageBytes

    var sampleSize = 1
    while (width / sampleSize > maxDimension * 2 || height / sampleSize > maxDimension * 2) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
        ?: return imageBytes

    val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
    val scaled = if (scale < 1f) {
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    } else {
        bitmap
    }

    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
    scaled.recycle()
    return output.toByteArray()
}
