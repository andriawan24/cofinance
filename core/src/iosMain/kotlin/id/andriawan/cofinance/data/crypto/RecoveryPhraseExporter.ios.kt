package id.andriawan.cofinance.data.crypto

import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionExpirationDate
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

/**
 * Pasteboard and the Files app on iOS.
 *
 * The pasteboard copy is given an expiry rather than being left indefinitely: a recovery phrase on a
 * general pasteboard is readable by whatever the user opens next, and on iOS that pasteboard also
 * follows the user onto their other devices through Universal Clipboard.
 *
 * Saving presents the document picker in export mode, so the words go wherever the user picks —
 * iCloud Drive, on-device storage, a third-party provider — rather than into a container the app
 * chose. The temporary file the picker exports from is what iOS hands over; the copy the user keeps
 * is at the location the picker returns.
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class IosRecoveryPhraseExporter : RecoveryPhraseExporter {

    override suspend fun copyToClipboard(text: String): Boolean = withContext(Dispatchers.Main) {
        UIPasteboard.generalPasteboard.setItems(
            listOf(mapOf(PASTEBOARD_PLAIN_TEXT to text)),
            options = mapOf(
                UIPasteboardOptionExpirationDate to
                        NSDate.dateWithTimeIntervalSinceNow(CLIPBOARD_LIFETIME_SECONDS)
            )
        )
        true
    }

    override suspend fun saveToFile(fileName: String, text: String): String? =
        withContext(Dispatchers.Main) {
            val fileUrl = writeTemporaryFile(fileName, text) ?: return@withContext null
            val rootViewController = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstNotNullOfOrNull { it.keyWindow?.rootViewController }
                ?: return@withContext null

            suspendCancellableCoroutine { continuation ->
                var delegate: DocumentExportDelegate?
                delegate = DocumentExportDelegate { savedUrl ->
                    delegate = null
                    if (continuation.isActive) continuation.resume(savedUrl?.path)
                }

                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(fileUrl))
                picker.delegate = delegate
                rootViewController.presentViewController(picker, animated = true, completion = null)
            }
        }

    private fun writeTemporaryFile(fileName: String, text: String): NSURL? {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        val written = NSString.create(string = text).writeToURL(
            url = url,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        return url.takeIf { written }
    }

    private companion object {
        const val PASTEBOARD_PLAIN_TEXT = "public.utf8-plain-text"
        const val CLIPBOARD_LIFETIME_SECONDS = 10.0 * 60.0
    }
}

private class DocumentExportDelegate(
    private val onFinished: (NSURL?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        onFinished(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onFinished(null)
    }
}

actual fun createRecoveryPhraseExporter(): RecoveryPhraseExporter = IosRecoveryPhraseExporter()
