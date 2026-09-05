package id.andriawan.cofinance.data.ocr

import cnames.structs.CGImage
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * iOS [OcrEngine] backed by the Vision framework's `VNRecognizeTextRequest`.
 *
 * Recognition runs at the accurate level with language correction disabled:
 * correction improves prose but corrupts receipt tokens such as amounts and
 * account numbers, which is exactly what the shared parser consumes.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class VisionOcrEngine : OcrEngine {

    override suspend fun recognize(image: ByteArray): OcrResult = withContext(Dispatchers.Default) {
        if (image.isEmpty()) return@withContext OcrResult()
        val cgImage = image.toUprightCGImage() ?: return@withContext OcrResult()
        recognizeCGImage(cgImage)
    }

    private fun recognizeCGImage(cgImage: CPointer<CGImage>): OcrResult {
        val request = VNRecognizeTextRequest().apply {
            recognitionLevel = VNRequestTextRecognitionLevelAccurate
            usesLanguageCorrection = false
        }

        val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any>())

        memScoped {
            val errorRef = alloc<ObjCObjectVar<NSError?>>()
            val performed = handler.performRequests(listOf(request), error = errorRef.ptr)
            if (!performed) {
                val message = errorRef.value?.localizedDescription ?: "Vision text recognition failed"
                error(message)
            }
        }

        val lines = request.results
            .orEmpty()
            .filterIsInstance<VNRecognizedTextObservation>()
            .mapNotNull { observation ->
                val candidate = observation.topCandidates(1UL).firstOrNull() as? VNRecognizedText
                    ?: return@mapNotNull null
                OcrLine(
                    text = candidate.string,
                    boundingBox = observation.normalizedRect(),
                    confidence = candidate.confidence
                )
            }
            .sortedWith(compareBy({ it.boundingBox.top }, { it.boundingBox.left }))

        if (lines.isEmpty()) return OcrResult()

        return OcrResult(
            blocks = listOf(
                OcrBlock(
                    text = lines.joinToString(separator = "\n", transform = OcrLine::text),
                    lines = lines
                )
            )
        )
    }

    private fun VNRecognizedTextObservation.normalizedRect(): OcrRect =
        boundingBox.useContents {
            visionBoxToTopLeftRect(
                minX = origin.x,
                minY = origin.y,
                width = size.width,
                height = size.height
            )
        }

    /**
     * Decodes the image bytes and redraws them upright, because `UIImage.CGImage`
     * exposes the raw bitmap without the EXIF orientation a camera capture carries.
     * Drawing through `UIImage.drawInRect` applies that orientation, so Vision sees
     * the receipt the same way the user did.
     */
    private fun ByteArray.toUprightCGImage(): CPointer<CGImage>? {
        val data: NSData = usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }

        val decoded = UIImage(data = data)
        val width = decoded.size.useContents { width }
        val height = decoded.size.useContents { height }
        if (width <= 0.0 || height <= 0.0) return null

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, 1.0)
        decoded.drawInRect(CGRectMake(0.0, 0.0, width, height))
        val upright = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return (upright ?: decoded).CGImage
    }
}

/**
 * Converts one Vision bounding box to the shared [OcrRect] convention.
 *
 * Vision reports normalized geometry with a **bottom-left** origin and Y growing
 * **upward**, while [OcrRect] uses a **top-left** origin with Y growing **downward**.
 * The Y axis is therefore flipped and the two Y edges swap roles:
 *
 * - the box's maximum Y — its *visual top* — becomes `top = 1 - maxY`
 * - the box's minimum Y — its *visual bottom* — becomes `bottom = 1 - minY`
 *
 * X needs no conversion: both conventions measure it left-to-right from the left edge.
 *
 * Getting the flip backwards would put every receipt's header at the bottom and
 * invert the parser's "totals sit low on the receipt" heuristic while still
 * producing plausible-looking 0..1 numbers, so it is isolated here and tested
 * directly rather than only through a live recognition run.
 *
 * Kept top-level and `internal` so it is reachable from `iosTest` without a device,
 * an image, or the Vision framework.
 */
internal fun visionBoxToTopLeftRect(
    minX: Double,
    minY: Double,
    width: Double,
    height: Double
): OcrRect {
    val maxX = minX + width
    val maxY = minY + height

    return OcrRect(
        left = minX.toFloat().clampToUnit(),
        top = (1.0 - maxY).toFloat().clampToUnit(),
        right = maxX.toFloat().clampToUnit(),
        bottom = (1.0 - minY).toFloat().clampToUnit()
    )
}

private fun Float.clampToUnit(): Float = coerceIn(0f, 1f)
