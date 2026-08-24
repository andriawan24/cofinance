package id.andriawan.cofinance.data.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RecoveryPhraseTest {

    @Test
    fun generationProducesTwelveWordsFromTheWordlist() = runTest {
        val phrase = RecoveryPhrase.generate()

        assertEquals(RecoveryPhrase.WORD_COUNT, phrase.words.size)
        phrase.words.forEach { word ->
            assertTrue(Bip39EnglishWordlist.indexOf(word) >= 0, "'$word' is not in the wordlist")
        }
        assertEquals(RecoveryPhrase.ENTROPY_BYTES, phrase.toEntropy().size)
    }

    @Test
    fun aGeneratedPhraseValidates() = runTest {
        val phrase = RecoveryPhrase.generate()

        val parsed = RecoveryPhrase.parse(phrase.text)

        assertIs<RecoveryPhraseResult.Valid>(parsed)
        assertEquals(phrase.words, parsed.phrase.words)
    }

    @Test
    fun entropyRoundTripsThroughThePhrase() = runTest {
        repeat(32) {
            val phrase = RecoveryPhrase.generate()

            val parsed = RecoveryPhrase.parse(phrase.text)

            assertIs<RecoveryPhraseResult.Valid>(parsed)
            assertContentEquals(phrase.toEntropy(), parsed.phrase.toEntropy())
        }
    }

    @Test
    fun twoGenerationsDiffer() = runTest {
        val first = RecoveryPhrase.generate()
        val second = RecoveryPhrase.generate()

        assertNotEquals(first.text, second.text)
    }

    @Test
    fun anAlteredWordIsRejectedByTheChecksum() = runTest {
        // Every word here is a real wordlist word, so only the checksum can catch the substitution.
        val altered = OZONE_PHRASE.replaceFirst("drill", "abandon")

        assertIs<RecoveryPhraseResult.ChecksumFailed>(RecoveryPhrase.parse(altered))
    }

    @Test
    fun almostEverySingleWordSubstitutionIsRejected() = runTest {
        val words = OZONE_PHRASE.split(" ")
        var rejected = 0

        // A 4-bit checksum cannot do better than catching 15 of every 16 alterations: the remaining
        // one in sixteen lands on a different phrase that is itself well-formed. That is a property
        // of BIP-0039 rather than of this implementation, so the bar is a rate, not a total.
        Bip39EnglishWordlist.words.forEach { replacement ->
            if (replacement == words.last()) return@forEach
            val candidate = (words.dropLast(1) + replacement).joinToString(" ")
            if (RecoveryPhrase.parse(candidate) is RecoveryPhraseResult.ChecksumFailed) rejected++
        }

        assertTrue(
            rejected >= 1_900,
            "Only $rejected of ${Bip39EnglishWordlist.SIZE - 1} substitutions were rejected"
        )
    }

    @Test
    fun aWordOutsideTheWordlistIsDistinguishableFromAChecksumFailure() = runTest {
        val typo = OZONE_PHRASE.replaceFirst("curtain", "curtian")

        val parsed = RecoveryPhrase.parse(typo)

        assertIs<RecoveryPhraseResult.UnknownWords>(parsed)
        assertEquals(
            listOf(RecoveryPhraseResult.UnknownWord(position = 5, word = "curtian")),
            parsed.words
        )
    }

    @Test
    fun theWrongNumberOfWordsIsRejected() = runTest {
        val short = OZONE_PHRASE.split(" ").dropLast(1).joinToString(" ")

        val parsed = RecoveryPhrase.parse(short)

        assertIs<RecoveryPhraseResult.WrongWordCount>(parsed)
        assertEquals(11, parsed.actual)
    }

    @Test
    fun noWordsAtAllIsAWordCountFailureRatherThanACrash() = runTest {
        val parsed = RecoveryPhrase.parse("   ")

        assertIs<RecoveryPhraseResult.WrongWordCount>(parsed)
        assertEquals(0, parsed.actual)
    }

    @Test
    fun entryIsNormalizedForCaseAndWhitespace() = runTest {
        // What a phone's autocapitalization and a phrase copied off paper actually produce.
        val messy = "  Ozone\tDRILL grab\nfiber   curtain grace pudding thank " +
            "cruise elder eight picnic  "

        val parsed = RecoveryPhrase.parse(messy)

        assertIs<RecoveryPhraseResult.Valid>(parsed)
        assertEquals(OZONE_PHRASE, parsed.phrase.text)
    }

    @Test
    fun aPhraseDoesNotReproduceItselfInToString() = runTest {
        val phrase = RecoveryPhrase.generate()

        // Asserted as an exact constant rather than by searching the description for phrase words.
        // Any search is unsound here: the description is English and so is the wordlist, and `over`,
        // `word`, `words`, and `phrase` are all BIP-0039 entries that occur inside the literal
        // "RecoveryPhrase(12 words)". A substring check failed on roughly one generated phrase in
        // fifty, and a word-boundary check still collides on `words`, in both cases reporting a leak
        // that never happened. Pinning the whole string is stronger anyway: it admits no
        // phrase-derived content at all, and it cannot flake.
        assertEquals("RecoveryPhrase(${RecoveryPhrase.WORD_COUNT} words)", phrase.toString())
    }

    /**
     * The published BIP-0039 English test vectors, entropy to mnemonic, taken verbatim from the
     * reference test set at https://github.com/trezor/python-mnemonic/blob/master/vectors.json,
     * which is the vector file BIP-0039 itself points to. These pin the encoding to the standard
     * rather than to this implementation: a phrase this app generates has to be readable by any
     * other BIP-0039 tool, and vice versa, or a user's written-down phrase means nothing outside it.
     */
    @Test
    fun officialBip39VectorsEncodeExactly() = runTest {
        VECTORS.forEach { (entropyHex, expected) ->
            val phrase = RecoveryPhrase.fromEntropy(entropyHex.hexToBytes())

            assertEquals(expected, phrase.text, "Vector $entropyHex encoded incorrectly")
        }
    }

    @Test
    fun officialBip39VectorsDecodeBackToTheirEntropy() = runTest {
        VECTORS.forEach { (entropyHex, mnemonic) ->
            val parsed = RecoveryPhrase.parse(mnemonic)

            assertIs<RecoveryPhraseResult.Valid>(parsed, "Vector $entropyHex was not accepted")
            assertContentEquals(entropyHex.hexToBytes(), parsed.phrase.toEntropy())
        }
    }

    private fun String.hexToBytes(): ByteArray = ByteArray(length / 2) { index ->
        substring(index * 2, index * 2 + 2).toInt(radix = 16).toByte()
    }

    private companion object {
        const val OZONE_PHRASE =
            "ozone drill grab fiber curtain grace pudding thank cruise elder eight picnic"

        val VECTORS = listOf(
            "00000000000000000000000000000000" to
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon " +
                "abandon abandon about",
            "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f" to
                "legal winner thank year wave sausage worth useful legal winner thank yellow",
            "80808080808080808080808080808080" to
                "letter advice cage absurd amount doctor acoustic avoid letter advice cage above",
            "ffffffffffffffffffffffffffffffff" to
                "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
            "9e885d952ad362caeb4efe34a8e91bd2" to OZONE_PHRASE,
            "c0ba5a8e914111210f2bd131f3d5e08d" to
                "scheme spot photo card baby mountain device kick cradle pact join borrow"
        )
    }
}
