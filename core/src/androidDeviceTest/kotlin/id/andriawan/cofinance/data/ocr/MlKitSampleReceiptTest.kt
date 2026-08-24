package id.andriawan.cofinance.data.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 1.3's verification: run [MlKitOcrEngine] over a **real** receipt photograph and
 * confirm it returns recognized lines with normalized top-left-origin geometry.
 *
 * The sample is not checked in. Receipts are personal financial records and this
 * repository's fixture policy forbids committing receipt image data, so the photograph
 * is supplied locally — see `README.md` in this directory.
 *
 * These tests fail rather than skip while the sample is absent. A skipped OCR check
 * reads as "verified" in a task list and in CI output, and this is the only evidence
 * that ML Kit reads real thermal print at all; the geometry contract alone is covered
 * by [MlKitGeometryTest], which needs no receipt.
 */
@RunWith(AndroidJUnit4::class)
class MlKitSampleReceiptTest {

    private val engine = MlKitOcrEngine()

    @Test
    fun recognizesLinesInASampleReceipt() = runTest {
        val result = engine.recognize(sampleReceiptBytes())

        assertTrue(
            result.lines.isNotEmpty(),
            "ML Kit recognized no lines in $SAMPLE_ASSET. Check the photograph is in focus, " +
                "upright, and shows the whole receipt."
        )
    }

    @Test
    fun reportsSampleReceiptGeometryInsideTheUnitRange() = runTest {
        val result = engine.recognize(sampleReceiptBytes())

        assertTrue(result.lines.isNotEmpty(), "No lines recognized in $SAMPLE_ASSET")

        result.lines.forEach { line ->
            val box = line.boundingBox
            listOf(box.left, box.top, box.right, box.bottom).forEach { edge ->
                assertTrue(edge in 0f..1f, "Edge $edge of '${line.text}' escaped 0..1")
            }
            assertTrue(box.top <= box.bottom, "top must not exceed bottom for '${line.text}'")
            assertTrue(box.left <= box.right, "left must not exceed right for '${line.text}'")
        }
    }

    /**
     * A receipt is taller than it is wide and its text spans the page, so recognized
     * content must not all collapse into one horizontal band. This catches a receipt
     * photographed sideways, or geometry normalized against the wrong axis.
     */
    @Test
    fun spreadsSampleReceiptLinesDownThePage() = runTest {
        val result = engine.recognize(sampleReceiptBytes())

        val tops = result.lines.map { it.boundingBox.top }
        assertTrue(tops.isNotEmpty(), "No lines recognized in $SAMPLE_ASSET")

        val spread = (tops.max() - tops.min())
        assertTrue(
            spread > 0.3f,
            "Recognized lines span only ${spread} of the image height. Expected a receipt's " +
                "text to run down the page — check the photograph is upright and complete."
        )
    }

    private fun sampleReceiptBytes(): ByteArray {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets

        return runCatching { assets.open(SAMPLE_ASSET).use { it.readBytes() } }
            .getOrElse {
                throw AssertionError(
                    """
                    Missing test asset: $SAMPLE_ASSET

                    Task 1.3 of the on-device-receipt-scanning change requires running ML Kit over a
                    real receipt. Supply the photograph at:

                        composeApp/src/androidDeviceTest/assets/$SAMPLE_ASSET

                    It is intentionally not committed — see the README beside this test. A rendered or
                    synthesized image must not be substituted: it would show only that ML Kit reads
                    cleanly rasterized type, which is not the claim this task makes.
                    """.trimIndent()
                )
            }
    }

    private companion object {
        const val SAMPLE_ASSET = "sample_receipt.jpg"
    }
}
