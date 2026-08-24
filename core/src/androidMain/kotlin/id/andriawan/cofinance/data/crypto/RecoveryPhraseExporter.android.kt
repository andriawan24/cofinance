package id.andriawan.cofinance.data.crypto

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clipboard and Downloads on Android.
 *
 * The clip is flagged sensitive so the launcher does not paint the words into the paste toast on
 * Android 13 and later, and it is the only thing this class can do about a clipboard other apps can
 * read: the phrase is on it until something replaces it, which is why the screen says so.
 *
 * The file goes to the public Downloads collection through MediaStore on Android 10 and later, which
 * needs no storage permission and puts it where a user looking for a download would look. Below
 * that, MediaStore has no Downloads collection and the public directory needs a runtime permission
 * this flow has no business asking for, so it lands in the app's own external Downloads directory —
 * still reachable over USB and by a file manager, still gone when the app is uninstalled.
 */
private class AndroidRecoveryPhraseExporter : RecoveryPhraseExporter {

    override suspend fun copyToClipboard(text: String): Boolean = withContext(Dispatchers.Main) {
        val context = context() ?: return@withContext false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return@withContext false

        val clip = ClipData.newPlainText(CLIP_LABEL, text).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }

        try {
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: RuntimeException) {
            // A clipboard that refuses the clip - a transaction too large, an OEM restriction - is a
            // failed copy, not a crash on a screen holding the only copy of the phrase.
            false
        }
    }

    override suspend fun saveToFile(fileName: String, text: String): String? =
        withContext(Dispatchers.IO) {
            val context = context() ?: return@withContext null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveThroughMediaStore(context, fileName, text)
                } else {
                    saveToAppExternalDownloads(context, fileName, text)
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }

    private fun saveThroughMediaStore(context: Context, fileName: String, text: String): String? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null
        resolver.openOutputStream(uri)?.use { it.write(text.encodeToByteArray()) } ?: return null
        return "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
    }

    private fun saveToAppExternalDownloads(
        context: Context,
        fileName: String,
        text: String
    ): String? {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return null
        directory.mkdirs()
        val file = File(directory, fileName)
        file.writeText(text)
        return file.absolutePath
    }

    private fun context(): Context? = DeviceKeyVaultStorage.applicationContext

    private companion object {
        const val CLIP_LABEL = "Cofinance recovery phrase"
        const val MIME_TYPE = "text/plain"
    }
}

actual fun createRecoveryPhraseExporter(): RecoveryPhraseExporter = AndroidRecoveryPhraseExporter()
