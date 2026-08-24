package id.andriawan.cofinance.data.ocr

import kotlinx.serialization.Serializable

/**
 * Text recognized from a receipt image by the platform OCR engine.
 *
 * Geometry is normalized to the 0..1 range against the source image and uses a
 * top-left origin on every platform, so shared parsing logic never sees platform
 * coordinate conventions or image resolution.
 */
@Serializable
data class OcrResult(val blocks: List<OcrBlock> = emptyList()) {
    val lines: List<OcrLine> get() = blocks.flatMap(OcrBlock::lines)
}

@Serializable
data class OcrBlock(
    val text: String = "",
    val lines: List<OcrLine> = emptyList()
)

@Serializable
data class OcrLine(
    val text: String = "",
    val boundingBox: OcrRect = OcrRect(),
    val confidence: Float? = null
)

/**
 * Normalized bounding box with a top-left origin: [top] is smaller for content
 * nearer the top of the image.
 */
@Serializable
data class OcrRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val centerY: Float get() = (top + bottom) / 2f
}

/** Recognizes text in a receipt image. Implemented per platform. */
fun interface OcrEngine {
    suspend fun recognize(image: ByteArray): OcrResult
}
