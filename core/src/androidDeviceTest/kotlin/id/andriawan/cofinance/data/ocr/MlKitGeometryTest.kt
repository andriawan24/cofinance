package id.andriawan.cofinance.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies [MlKitOcrEngine]'s coordinate contract on a device.
 *
 * The input here is a **plain rendered canvas, not a receipt**. It exists to pin the
 * geometry conversion — normalization to 0..1, top-left origin, and EXIF-upright
 * decoding — which is machine-checkable without any real receipt. It deliberately
 * says nothing about whether ML Kit reads real thermal receipts well; that is what
 * [MlKitSampleReceiptTest] and the fixture corpus are for.
 */
@RunWith(AndroidJUnit4::class)
class MlKitGeometryTest {

    private val engine = MlKitOcrEngine()

    @Test
    fun normalizesEveryBoundingBoxIntoTheUnitRange() = runTest {
        val result = engine.recognize(uprightCanvas().toJpegBytes())

        assertTrue(result.lines.isNotEmpty(), "Expected at least one recognized line")

        result.lines.forEach { line ->
            val box = line.boundingBox
            listOf(box.left, box.top, box.right, box.bottom).forEach { edge ->
                assertTrue(edge in 0f..1f, "Edge $edge of '${line.text}' escaped 0..1")
            }
            assertTrue(box.left <= box.right, "left must not exceed right for '${line.text}'")
            assertTrue(box.top <= box.bottom, "top must not exceed bottom for '${line.text}'")
        }
    }

    @Test
    fun placesATopTokenAboveABottomTokenInTopLeftOriginCoordinates() = runTest {
        val result = engine.recognize(uprightCanvas().toJpegBytes())

        val top = result.requireTopOf(TOP_TOKEN)
        val bottom = result.requireTopOf(BOTTOM_TOKEN)

        assertTrue(
            top < bottom,
            "'$TOP_TOKEN' sits at the top of the image so it must have the smaller `top`; " +
                "got $TOP_TOKEN=$top $BOTTOM_TOKEN=$bottom"
        )
        assertTrue(top < 0.5f, "'$TOP_TOKEN' should land in the upper half, got $top")
        assertTrue(bottom > 0.5f, "'$BOTTOM_TOKEN' should land in the lower half, got $bottom")
    }

    /**
     * A camera capture stores unrotated pixels plus an EXIF orientation tag. Without
     * honouring that tag the receipt is recognized sideways and "top" silently means
     * the side of the page, so the ordering above must survive a rotated capture.
     */
    @Test
    fun honoursExifOrientationSoRotatedCapturesStillReadTopToBottom() = runTest {
        val result = engine.recognize(uprightCanvas().asRotatedCapture())

        val top = result.requireTopOf(TOP_TOKEN)
        val bottom = result.requireTopOf(BOTTOM_TOKEN)

        assertTrue(
            top < bottom,
            "EXIF-rotated capture must still order top-to-bottom; got $TOP_TOKEN=$top $BOTTOM_TOKEN=$bottom"
        )
    }

    private fun OcrResult.requireTopOf(token: String): Float {
        val line = lines.firstOrNull { it.text.contains(token, ignoreCase = true) }
            ?: error("'$token' was not recognized. Recognized lines: ${lines.map(OcrLine::text)}")
        return line.boundingBox.top
    }

    /** A white page with one token near the top edge and one near the bottom edge. */
    private fun uprightCanvas(): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 96f
        }
        canvas.drawText(TOP_TOKEN, 80f, 200f, paint)
        canvas.drawText(BOTTOM_TOKEN, 80f, HEIGHT - 140f, paint)

        return bitmap
    }

    /**
     * Re-encodes the canvas the way a portrait camera capture stores it: pixels rotated
     * a quarter turn backwards, with `ORIENTATION_ROTATE_90` recording how to undo that.
     * A decoder that ignores EXIF therefore sees the tokens side by side rather than
     * stacked, which is exactly the failure this guards.
     */
    private fun Bitmap.asRotatedCapture(): ByteArray {
        val stored = Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            Matrix().apply { postRotate(270f) },
            true
        )

        val context = InstrumentationRegistry.getInstrumentation().context
        val file = File.createTempFile("rotated-capture", ".jpg", context.cacheDir)
        file.outputStream().use { stored.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        return file.readBytes().also { file.delete() }
    }

    private fun Bitmap.toJpegBytes(): ByteArray =
        ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 1400
        const val TOP_TOKEN = "ALPHA"
        const val BOTTOM_TOKEN = "OMEGA"
    }
}
