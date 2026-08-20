package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * The 12-word BIP-0039 phrase that is the user's only route back to their synchronized data.
 *
 * 12 words carry 128 bits of entropy, which matches the security level of everything the phrase
 * wraps, so the extra words of a 24-word phrase would protect nothing while being materially harder
 * to transcribe by hand. The entropy is what the wrapping layer derives from; the words exist so
 * that a human can write it down and read it back, and the checksum exists so that a mistake made
 * doing so is caught at entry rather than surfacing as an unopenable wrap.
 *
 * A phrase is held in memory only. Nothing here writes it anywhere, and nothing here logs it.
 */
class RecoveryPhrase private constructor(
    /** The 12 words in order. Position is part of the encoding, so the list must not be reordered. */
    val words: List<String>,
    private val entropy: ByteArray
) {

    /** The phrase as a single space-separated line, for display and for the confirmation step. */
    val text: String get() = words.joinToString(separator = " ")

    /**
     * Returns the 128 bits of entropy this phrase encodes, for the wrapping layer to derive a key
     * from. A fresh copy is returned each call so a caller that zeroes its copy cannot blank the
     * phrase for everybody else.
     */
    fun toEntropy(): ByteArray = entropy.copyOf()

    /** Describes the phrase without reproducing any of its words. */
    override fun toString(): String = "RecoveryPhrase($WORD_COUNT words)"

    companion object {
        /** Fixed by Decision 5: 12 words, 128 bits, 4 checksum bits. */
        const val WORD_COUNT: Int = 12
        const val ENTROPY_BYTES: Int = 16

        /** Each word encodes one index into the 2048-entry wordlist, so 11 bits per word. */
        private const val INDEX_BITS = 11
        private const val INDEX_MASK = (1 shl INDEX_BITS) - 1

        /** BIP-0039 appends entropy-length-in-bits / 32 checksum bits; 128 / 32 is 4. */
        private const val CHECKSUM_BITS = ENTROPY_BYTES * 8 / 32

        private val digest get() = CryptographyProvider.Default.get(SHA256)

        /** Runs of any whitespace separate words, so a phrase pasted across lines still parses. */
        private val WHITESPACE = Regex("\\s+")

        /** Generates a new phrase from 128 bits drawn from the platform CSPRNG. */
        suspend fun generate(): RecoveryPhrase =
            fromEntropy(CryptographyRandom.nextBytes(ENTROPY_BYTES))

        /**
         * Encodes [entropy] as a phrase. Exposed for the published BIP-0039 test vectors, which pin
         * this encoding to the standard rather than to this implementation.
         */
        suspend fun fromEntropy(entropy: ByteArray): RecoveryPhrase {
            require(entropy.size == ENTROPY_BYTES) { "Entropy must be $ENTROPY_BYTES bytes" }
            return RecoveryPhrase(encode(entropy), entropy.copyOf())
        }

        /**
         * Parses a phrase the user typed.
         *
         * Input is normalized before anything is looked up: surrounding whitespace is trimmed, runs
         * of whitespace between words are collapsed, and words are lowercased. All three are safe
         * because the BIP-0039 English wordlist is lowercase ASCII with no multi-word entries, so
         * normalization can only turn an input that a human would call correct into one that
         * matches — it can never turn one valid phrase into a different valid phrase. It is worth
         * doing because phones capitalize the first word of a text field by default and a phrase
         * read off paper arrives with arbitrary spacing and line breaks; without this, a user who
         * typed their phrase perfectly would be told it was wrong.
         *
         * The result distinguishes the three ways entry fails so the screen can say which one
         * happened: the wrong number of words, words that are not in the list at all, or the right
         * words with a checksum that does not hold — which is what a swapped or transposed word
         * looks like.
         */
        suspend fun parse(input: String): RecoveryPhraseResult {
            val words = input.trim().split(WHITESPACE)
                .filter(String::isNotEmpty)
                .map(String::lowercase)

            if (words.size != WORD_COUNT) return RecoveryPhraseResult.WrongWordCount(words.size)

            val indices = IntArray(WORD_COUNT)
            val unknown = mutableListOf<RecoveryPhraseResult.UnknownWord>()
            words.forEachIndexed { position, word ->
                val index = Bip39EnglishWordlist.indexOf(word)
                if (index < 0) {
                    unknown += RecoveryPhraseResult.UnknownWord(position = position + 1, word = word)
                } else {
                    indices[position] = index
                }
            }
            if (unknown.isNotEmpty()) return RecoveryPhraseResult.UnknownWords(unknown)

            val entropy = ByteArray(ENTROPY_BYTES)
            val checksum = decode(indices, entropy)
            if (checksum != checksumOf(entropy)) return RecoveryPhraseResult.ChecksumFailed

            return RecoveryPhraseResult.Valid(RecoveryPhrase(words, entropy))
        }

        /**
         * Appends the checksum byte to [entropy] and reads the concatenation 11 bits at a time. The
         * 136 available bits yield 12 indices and leave 4 unread, which is exactly the discipline
         * BIP-0039 describes as taking the leading 4 checksum bits.
         */
        private suspend fun encode(entropy: ByteArray): List<String> {
            val source = entropy + digest.hasher().hash(entropy)[0]
            val words = ArrayList<String>(WORD_COUNT)
            var accumulator = 0
            var pending = 0
            for (byte in source) {
                accumulator = (accumulator shl 8) or (byte.toInt() and 0xFF)
                pending += 8
                while (pending >= INDEX_BITS && words.size < WORD_COUNT) {
                    pending -= INDEX_BITS
                    words += Bip39EnglishWordlist.words[(accumulator ushr pending) and INDEX_MASK]
                }
            }
            return words
        }

        /**
         * The inverse of [encode]: fills [entropy] from the 11-bit indices and returns the 4 bits
         * left over, which are the checksum the phrase claims.
         */
        private fun decode(indices: IntArray, entropy: ByteArray): Int {
            var accumulator = 0
            var pending = 0
            var written = 0
            for (index in indices) {
                accumulator = (accumulator shl INDEX_BITS) or index
                pending += INDEX_BITS
                while (pending >= 8 && written < entropy.size) {
                    pending -= 8
                    entropy[written++] = ((accumulator ushr pending) and 0xFF).toByte()
                }
            }
            return accumulator and ((1 shl pending) - 1)
        }

        /** The leading [CHECKSUM_BITS] bits of SHA-256 over the entropy. */
        private suspend fun checksumOf(entropy: ByteArray): Int =
            (digest.hasher().hash(entropy)[0].toInt() and 0xFF) ushr (8 - CHECKSUM_BITS)
    }
}

/**
 * The outcome of parsing a phrase the user typed.
 *
 * The failures are kept apart because the screen has something different to say about each: an
 * unknown word is a spelling problem the user can see and fix, whereas a checksum failure means
 * every word is real but at least one is the wrong real word, which the user cannot spot by reading.
 */
sealed interface RecoveryPhraseResult {

    /** The phrase parsed and its checksum holds. */
    data class Valid(val phrase: RecoveryPhrase) : RecoveryPhraseResult

    /** [actual] words were entered where [RecoveryPhrase.WORD_COUNT] were expected. */
    data class WrongWordCount(val actual: Int) : RecoveryPhraseResult

    /** One or more entered words are not in the BIP-0039 English wordlist. */
    data class UnknownWords(val words: List<UnknownWord>) : RecoveryPhraseResult

    /** Every word is in the wordlist, but the phrase as a whole does not check out. */
    data object ChecksumFailed : RecoveryPhraseResult

    /** A word that is not in the wordlist, at its 1-based [position] in the entered phrase. */
    data class UnknownWord(val position: Int, val word: String)
}
