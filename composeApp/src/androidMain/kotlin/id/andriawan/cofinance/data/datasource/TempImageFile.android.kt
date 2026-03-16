package id.andriawan.cofinance.data.datasource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

actual fun compressImage(image: ByteArray, maxWidth: Int, quality: Int): ByteArray {
    val original = BitmapFactory.decodeByteArray(image, 0, image.size) ?: return image

    val scaleFactor = if (original.width > maxWidth) {
        maxWidth.toFloat() / original.width
    } else {
        1f
    }

    val scaledBitmap = if (scaleFactor < 1f) {
        val newHeight = (original.height * scaleFactor).toInt()
        Bitmap.createScaledBitmap(original, maxWidth, newHeight, true).also {
            original.recycle()
        }
    } else {
        original
    }

    return ByteArrayOutputStream().use { stream ->
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        scaledBitmap.recycle()
        stream.toByteArray()
    }
}

actual fun saveTempImage(image: ByteArray): String {
    val tempFile = File.createTempFile("receipt_", ".jpg")
    tempFile.writeBytes(image)
    return tempFile.absolutePath
}

actual fun deleteTempFile(path: String) {
    File(path).delete()
}
