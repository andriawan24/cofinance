package id.andriawan.cofinance.data.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android [OcrEngine] backed by ML Kit Text Recognition v2, bundled rather than
 * delivered by Play services so a first scan cannot fail on a model download.
 */
class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: ByteArray): OcrResult {
        val bitmap = withContext(Dispatchers.Default) { image.decodeUpright() }
            ?: throw IllegalArgumentException("Receipt image bytes could not be decoded")

        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result: Text -> continuation.resume(result) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }

        return OcrResult(
            blocks = text.textBlocks.map { block ->
                OcrBlock(
                    text = block.text,
                    lines = block.lines.map { line ->
                        OcrLine(
                            text = line.text,
                            boundingBox = line.boundingBox.normalize(bitmap.width, bitmap.height),
                            confidence = line.confidence
                        )
                    }
                )
            }
        )
    }
}

/**
 * ML Kit reports pixel-space boxes against a **top-left** origin, which already
 * matches [OcrRect]'s convention, so no axis is flipped here — normalization only
 * divides by the source image dimensions. Contrast `VisionOcrEngine` on iOS, which
 * must flip Y because Vision's origin is bottom-left.
 *
 * Values are clamped because ML Kit may report a box that overhangs the image edge
 * by a pixel, and the shared geometry contract is a strict 0..1.
 */
private fun Rect?.normalize(imageWidth: Int, imageHeight: Int): OcrRect {
    if (this == null || imageWidth <= 0 || imageHeight <= 0) return OcrRect()

    return OcrRect(
        left = (left / imageWidth.toFloat()).clampToUnit(),
        top = (top / imageHeight.toFloat()).clampToUnit(),
        right = (right / imageWidth.toFloat()).clampToUnit(),
        bottom = (bottom / imageHeight.toFloat()).clampToUnit()
    )
}

private fun Float.clampToUnit(): Float = coerceIn(0f, 1f)

/**
 * Decodes the image bytes and rotates them upright, because [BitmapFactory] ignores
 * the EXIF orientation a camera capture carries. Without this a portrait photo
 * decodes sideways, which both degrades recognition and makes "top" mean the side
 * of the receipt — silently breaking the parser's positional amount heuristics.
 *
 * Mirrored EXIF orientations are left alone; camera captures do not produce them.
 */
private fun ByteArray.decodeUpright(): Bitmap? {
    val decoded = BitmapFactory.decodeByteArray(this, 0, size) ?: return null
    val degrees = exifRotationDegrees()
    if (degrees == 0) return decoded

    val rotated = Bitmap.createBitmap(
        decoded,
        0,
        0,
        decoded.width,
        decoded.height,
        Matrix().apply { postRotate(degrees.toFloat()) },
        true
    )
    if (rotated !== decoded) decoded.recycle()
    return rotated
}

private fun ByteArray.exifRotationDegrees(): Int = runCatching {
    val orientation = ExifInterface(ByteArrayInputStream(this))
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}.getOrDefault(0)
