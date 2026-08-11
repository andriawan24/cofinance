package id.andriawan.cofinance.data.ocr

import android.graphics.BitmapFactory
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: ByteArray): OcrResult {
        val bitmap = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(image, 0, image.size)
        } ?: throw IllegalArgumentException("Receipt image bytes could not be decoded")

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
 * ML Kit reports pixel-space boxes against a top-left origin, matching [OcrRect]'s
 * convention, so normalization only divides by the source image dimensions.
 */
private fun Rect?.normalize(imageWidth: Int, imageHeight: Int): OcrRect {
    if (this == null || imageWidth <= 0 || imageHeight <= 0) return OcrRect()

    return OcrRect(
        left = left / imageWidth.toFloat(),
        top = top / imageHeight.toFloat(),
        right = right / imageWidth.toFloat(),
        bottom = bottom / imageHeight.toFloat()
    )
}
