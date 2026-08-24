package id.andriawan.cofinance.data.crypto

/**
 * Getting the twelve words off the screen and somewhere the user actually keeps them.
 *
 * Setup used to prove the phrase had been written down by asking for three words back. That proved
 * little — the words were still on the screen above — and cost every user a typing exercise, so the
 * check is gone and this takes its place: the phrase can be put on the clipboard or written to a
 * file in one tap, and saving it is the user's decision rather than a gate.
 *
 * Both operations hand the phrase to somewhere the app does not control, which is the point of a
 * recovery phrase: a copy that survives this device. The platform implementations do what each one
 * offers to limit the blast radius — marking the clip sensitive, giving it an expiry — but neither
 * pretends the copy is protected once it leaves.
 */
interface RecoveryPhraseExporter {

    /** Puts [text] on the system clipboard, marked sensitive where the platform understands that. */
    suspend fun copyToClipboard(text: String): Boolean

    /**
     * Writes [text] to a file the user can reach, named [fileName].
     *
     * Returns where it landed, in whatever form is meaningful to the user on that platform, or
     * `null` when nothing was written.
     */
    suspend fun saveToFile(fileName: String, text: String): String?
}

/**
 * What the last copy or save attempt did, shown next to the buttons and then dismissed.
 *
 * Saving is not a gate — setup finishes whether or not the user ever taps these — so a failure here
 * is a notice rather than a setup error: nothing about the setup itself has gone wrong.
 */
sealed interface PhraseExportStatus {

    /** The phrase is on the clipboard, where anything the user opens next can read it. */
    data object Copied : PhraseExportStatus

    /** The file was written, at [location] in whatever form the platform shows the user. */
    data class Saved(val location: String) : PhraseExportStatus

    /** The clipboard refused the phrase. Nothing was copied. */
    data object CopyFailed : PhraseExportStatus

    /** No file was written, either because the user backed out or because the write failed. */
    data object SaveFailed : PhraseExportStatus
}

/** The exact bytes that get copied or written: the numbered words, one per line. */
fun RecoveryPhrase.toExportText(): String = words.toRecoveryPhraseExportText()

/** Same formatting, for callers that hold the words rather than the phrase. */
fun List<String>.toRecoveryPhraseExportText(): String =
    mapIndexed { index, word -> "${index + 1}. $word" }.joinToString(separator = "\n")

/** File name used for a downloaded phrase. Constant, so a second save replaces the first. */
const val RECOVERY_PHRASE_FILE_NAME: String = "cofinance-recovery-phrase.txt"

expect fun createRecoveryPhraseExporter(): RecoveryPhraseExporter
