package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * The recovery phrase that is the user's only route back to their synchronized data: six groups of
 * four characters drawn from lowercase letters, uppercase letters and digits, such as
 * `k3Rm 9XaQ 2mNp 7fTz bW4h Ld6s`.
 *
 * The phrase carries 128 bits of entropy, which matches the security level of everything it wraps.
 * The entropy is what the wrapping layer derives from; the groups exist so that a human can write it
 * down and read it back, and the checksum exists so that a mistake made doing so is caught at entry
 * rather than surfacing as an unopenable wrap.
 *
 * Four characters of the 58-character alphabet hold the 22 bits each group encodes, and six groups
 * carry the 128 bits of entropy plus 4 checksum bits exactly, with nothing left over. The alphabet
 * omits `0`, `O`, `I` and `l` because the phrase is meant to be copied by hand, and those four are
 * the pairs a reader confuses. Everything else about the phrase is case-sensitive: unlike a wordlist
 * phrase, `k3Rm` and `K3rm` are different groups, so entry is not normalized beyond whitespace.
 *
 * A phrase is held in memory only. Nothing here writes it anywhere, and nothing here logs it.
 */
class RecoveryPhrase private constructor(
    val groups: List<String>,
    private val entropy: ByteArray
) {

    /** The phrase as a single space-separated line, for display and for the confirmation step. */
    val text: String get() = groups.joinToString(separator = " ")

    /**
     * Returns the 128 bits of entropy this phrase encodes, for the wrapping layer to derive a key
     * from. A fresh copy is returned each call so a caller that zeroes its copy cannot blank the
     * phrase for everybody else.
     */
    fun toEntropy(): ByteArray = entropy.copyOf()

    /** Describes the phrase without reproducing any of its groups. */
    override fun toString(): String = "RecoveryPhrase($GROUP_COUNT groups)"

    companion object {
        /** Six groups of four characters: 128 bits of entropy plus 4 checksum bits. */
        const val GROUP_COUNT: Int = 6
        const val GROUP_LENGTH: Int = 4
        const val ENTROPY_BYTES: Int = 16

        /**
         * The characters a group may contain, in the order that gives each its digit value.
         *
         * Lowercase, uppercase and digits, less `0`, `O`, `I` and `l`. Fifty-eight characters is
         * enough that four of them cover the 22 bits a group carries, so dropping the confusable
         * four costs nothing.
         */
        const val ALPHABET: String =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

        /** 6 groups * 22 bits is 132 bits, which is the entropy plus its checksum exactly. */
        private const val GROUP_BITS = 22
        private const val GROUP_MASK = (1 shl GROUP_BITS) - 1

        /** A four-character group can spell values up to 58^4, of which only these are phrases. */
        private const val GROUP_LIMIT = 1 shl GROUP_BITS

        /** Entropy-length-in-bits / 32 checksum bits, as BIP-0039 does it; 128 / 32 is 4. */
        private const val CHECKSUM_BITS = ENTROPY_BYTES * 8 / 32

        private val digest get() = CryptographyProvider.Default.get(SHA256)

        private val valueByChar: Map<Char, Int> by lazy {
            ALPHABET.withIndex().associate { (value, char) -> char to value }
        }

        /** Runs of any whitespace separate groups, so a phrase pasted across lines still parses. */
        private val WHITESPACE = Regex("\\s+")

        /** Generates a new phrase from 128 bits drawn from the platform CSPRNG. */
        suspend fun generate(): RecoveryPhrase =
            fromEntropy(CryptographyRandom.nextBytes(ENTROPY_BYTES))

        /** Encodes [entropy] as a phrase. Exposed so the encoding can be pinned by test vectors. */
        suspend fun fromEntropy(entropy: ByteArray): RecoveryPhrase {
            require(entropy.size == ENTROPY_BYTES) { "Entropy must be $ENTROPY_BYTES bytes" }
            return RecoveryPhrase(encode(entropy), entropy.copyOf())
        }

        /**
         * Parses a phrase the user typed.
         *
         * Input is normalized only where normalizing cannot change which phrase was meant:
         * surrounding whitespace is trimmed and runs of whitespace between groups are collapsed, so
         * a phrase read off paper with arbitrary spacing and line breaks still parses. Case is left
         * alone, because case carries entropy here — which is also why the entry field must not
         * autocapitalize.
         *
         * The result distinguishes the three ways entry fails so the screen can say which one
         * happened: the wrong number of groups, a group that could never be part of any phrase, or
         * six well-formed groups with a checksum that does not hold — which is what a mistyped or
         * transposed group looks like.
         */
        suspend fun parse(input: String): RecoveryPhraseResult {
            val groups = input.trim().split(WHITESPACE).filter(String::isNotEmpty)

            if (groups.size != GROUP_COUNT) return RecoveryPhraseResult.WrongGroupCount(groups.size)

            val values = IntArray(GROUP_COUNT)
            val malformed = mutableListOf<RecoveryPhraseResult.MalformedGroup>()
            groups.forEachIndexed { position, group ->
                val value = valueOf(group)
                if (value == null) {
                    malformed += RecoveryPhraseResult.MalformedGroup(
                        position = position + 1,
                        group = group
                    )
                } else {
                    values[position] = value
                }
            }
            if (malformed.isNotEmpty()) return RecoveryPhraseResult.MalformedGroups(malformed)

            val entropy = ByteArray(ENTROPY_BYTES)
            val checksum = decode(values, entropy)
            if (checksum != checksumOf(entropy)) return RecoveryPhraseResult.ChecksumFailed

            return RecoveryPhraseResult.Valid(RecoveryPhrase(groups, entropy))
        }

        /**
         * Appends the checksum byte to [entropy] and reads the concatenation 22 bits at a time. The
         * 136 available bits yield 6 groups and leave 4 unread, which is the same discipline
         * BIP-0039 describes as taking the leading 4 checksum bits.
         */
        private suspend fun encode(entropy: ByteArray): List<String> {
            val source = entropy + digest.hasher().hash(entropy)[0]
            val groups = ArrayList<String>(GROUP_COUNT)
            var accumulator = 0
            var pending = 0
            for (byte in source) {
                accumulator = (accumulator shl 8) or (byte.toInt() and 0xFF)
                pending += 8
                while (pending >= GROUP_BITS && groups.size < GROUP_COUNT) {
                    pending -= GROUP_BITS
                    groups += spell((accumulator ushr pending) and GROUP_MASK)
                }
            }
            return groups
        }

        /**
         * The inverse of [encode]: fills [entropy] from the 22-bit group values and returns the 4
         * bits left over, which are the checksum the phrase claims.
         */
        private fun decode(values: IntArray, entropy: ByteArray): Int {
            var accumulator = 0
            var pending = 0
            var written = 0
            for (value in values) {
                accumulator = (accumulator shl GROUP_BITS) or value
                pending += GROUP_BITS
                while (pending >= 8 && written < entropy.size) {
                    pending -= 8
                    entropy[written++] = ((accumulator ushr pending) and 0xFF).toByte()
                }
            }
            return accumulator and ((1 shl pending) - 1)
        }

        /** Writes [value] as [GROUP_LENGTH] alphabet characters, most significant first. */
        private fun spell(value: Int): String {
            val characters = CharArray(GROUP_LENGTH)
            var remaining = value
            for (position in GROUP_LENGTH - 1 downTo 0) {
                characters[position] = ALPHABET[remaining % ALPHABET.length]
                remaining /= ALPHABET.length
            }
            return characters.concatToString()
        }

        /**
         * The inverse of [spell], or null when [group] is not something [spell] could have written:
         * the wrong length, a character outside the alphabet, or a value above the 22 bits a group
         * carries — the last of which is possible because four characters can spell more values
         * than a group is allowed to hold.
         */
        private fun valueOf(group: String): Int? {
            if (group.length != GROUP_LENGTH) return null
            var value = 0
            for (character in group) {
                val digit = valueByChar[character] ?: return null
                value = value * ALPHABET.length + digit
            }
            return value.takeIf { it < GROUP_LIMIT }
        }

        /** The leading [CHECKSUM_BITS] bits of SHA-256 over the entropy. */
        private suspend fun checksumOf(entropy: ByteArray): Int =
            (digest.hasher().hash(entropy)[0].toInt() and 0xFF) ushr (8 - CHECKSUM_BITS)
    }
}

/**
 * The outcome of parsing a phrase the user typed.
 *
 * The failures are kept apart because the screen has something different to say about each: a
 * malformed group is a transcription problem the user can see and fix, whereas a checksum failure
 * means every group is well formed but at least one is the wrong group, which the user cannot spot
 * by reading.
 */
sealed interface RecoveryPhraseResult {

    /** The phrase parsed and its checksum holds. */
    data class Valid(val phrase: RecoveryPhrase) : RecoveryPhraseResult

    /** [actual] groups were entered where [RecoveryPhrase.GROUP_COUNT] were expected. */
    data class WrongGroupCount(val actual: Int) : RecoveryPhraseResult

    /** One or more entered groups are not something this app could ever have produced. */
    data class MalformedGroups(val groups: List<MalformedGroup>) : RecoveryPhraseResult

    /** Every group is well formed, but the phrase as a whole does not check out. */
    data object ChecksumFailed : RecoveryPhraseResult

    /** A group that is not a valid group, at its 1-based [position] in the entered phrase. */
    data class MalformedGroup(val position: Int, val group: String)
}
