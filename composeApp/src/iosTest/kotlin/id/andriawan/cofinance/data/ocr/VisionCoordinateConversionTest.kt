package id.andriawan.cofinance.data.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the Vision bottom-left to [OcrRect] top-left conversion.
 *
 * This does not replace the live-recognition check that task 1.4 requires; it
 * isolates the axis flip, which is the part that can be silently wrong while still
 * emitting well-formed 0..1 geometry.
 */
class VisionCoordinateConversionTest {

    @Test
    fun `flips the y axis so a box near the image top gets a small top value`() {
        // Vision, bottom-left origin: a box whose bottom edge is 90% of the way up
        // the image is visually near the TOP of the image.
        val nearTop = visionBoxToTopLeftRect(minX = 0.1, minY = 0.9, width = 0.2, height = 0.05)

        assertEquals(0.05f, nearTop.top, absoluteTolerance = 1e-5f)
        assertEquals(0.10f, nearTop.bottom, absoluteTolerance = 1e-5f)
        assertTrue(nearTop.top < 0.5f, "A box near the image top must have a small `top`")
    }

    @Test
    fun `flips the y axis so a box near the image bottom gets a large top value`() {
        // Bottom-left origin: minY near 0 means visually near the BOTTOM.
        val nearBottom = visionBoxToTopLeftRect(minX = 0.1, minY = 0.05, width = 0.2, height = 0.05)

        assertEquals(0.90f, nearBottom.top, absoluteTolerance = 1e-5f)
        assertEquals(0.95f, nearBottom.bottom, absoluteTolerance = 1e-5f)
        assertTrue(nearBottom.top > 0.5f, "A box near the image bottom must have a large `top`")
    }

    @Test
    fun `orders a top token above a bottom token after conversion`() {
        val header = visionBoxToTopLeftRect(minX = 0.1, minY = 0.88, width = 0.3, height = 0.06)
        val total = visionBoxToTopLeftRect(minX = 0.1, minY = 0.06, width = 0.3, height = 0.06)

        assertTrue(
            header.top < total.top,
            "Header at the receipt top must convert to a smaller `top` than a total at the bottom, " +
                "got header=${header.top} total=${total.top}"
        )
    }

    @Test
    fun `leaves the x axis untouched`() {
        val rect = visionBoxToTopLeftRect(minX = 0.25, minY = 0.4, width = 0.5, height = 0.1)

        assertEquals(0.25f, rect.left, absoluteTolerance = 1e-5f)
        assertEquals(0.75f, rect.right, absoluteTolerance = 1e-5f)
    }

    @Test
    fun `keeps every edge inside the unit range`() {
        // Vision occasionally reports a box overhanging the image edge.
        val overhanging = visionBoxToTopLeftRect(minX = -0.02, minY = -0.03, width = 1.06, height = 1.09)

        listOf(overhanging.left, overhanging.top, overhanging.right, overhanging.bottom)
            .forEach { edge -> assertTrue(edge in 0f..1f, "Edge $edge escaped 0..1") }
    }

    @Test
    fun `keeps top above bottom for every box`() {
        val rect = visionBoxToTopLeftRect(minX = 0.0, minY = 0.3, width = 1.0, height = 0.2)

        assertTrue(rect.top < rect.bottom, "`top` must be the smaller edge in a top-left origin")
        assertEquals(0.4f, rect.centerY, absoluteTolerance = 1e-5f)
    }
}
