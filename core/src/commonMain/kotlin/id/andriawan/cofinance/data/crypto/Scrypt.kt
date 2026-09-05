package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256

/**
 * Cost parameters for one [Scrypt] derivation.
 *
 * The parameters are caller-supplied rather than fixed so that the RFC 7914 published vectors can be
 * reproduced verbatim in tests, and so a future build can raise the cost without changing the
 * derivation itself. [PIN] is the set this app actually derives PIN key material under.
 */
data class ScryptParameters(
    val n: Int,
    val r: Int,
    val p: Int,
    val derivedKeyLength: Int
) {
    init {
        require(n > 1 && (n and (n - 1)) == 0) { "scrypt N must be a power of two greater than 1" }
        require(r > 0) { "scrypt r must be positive" }
        require(p > 0) { "scrypt p must be positive" }
        require(derivedKeyLength > 0) { "scrypt dkLen must be positive" }
        require(r.toLong() * p.toLong() < (1L shl 30)) { "scrypt r * p must be less than 2^30" }
        require(128L * r.toLong() * p.toLong() <= Int.MAX_VALUE) { "scrypt r and p are too large" }
        require(n.toLong() * 32L * r.toLong() <= Int.MAX_VALUE) { "scrypt N and r are too large" }
    }

    companion object {
        /**
         * Parameters for deriving key material from the six-digit app PIN.
         *
         * N=16384, r=8, p=1 is RFC 7914's own interactive-login set. It costs 128 * N * r = 16 MiB of
         * memory per guess, which is the property a PIN needs: a six-digit PIN has only 10^6 values,
         * so an attacker's limit has to be memory bandwidth rather than hash throughput.
         *
         * The floor is Android minSdk 24, so the budget is a 2016-era phone. 16 MiB is a transient
         * allocation that such a device can hold and release per unlock, well inside a normal app
         * heap, and p=1 keeps the derivation a single pass rather than 16 sequential ones. Raising N
         * to 32768 would double both the wall time on that floor device and the peak allocation to
         * buy an attacker one bit; the remaining margin comes instead from composing this output with
         * the non-extractable device secret, which is what makes guessing require the device at all.
         *
         * 32 bytes matches the AES-256 key this composes into.
         */
        val PIN = ScryptParameters(n = 16384, r = 8, p = 1, derivedKeyLength = 32)
    }
}

/**
 * scrypt, as defined in [RFC 7914](https://www.rfc-editor.org/rfc/rfc7914).
 *
 * The cryptography library provides PBKDF2 and HKDF but no memory-hard derivation, and no Kotlin
 * Multiplatform library covering both Android and Apple provides one either. RFC 7914 defines scrypt
 * entirely in terms of PBKDF2-HMAC-SHA256 plus Salsa20/8, so the gap is closed here in common code
 * rather than through platform bindings — which keeps the derivation verifiable in `commonTest`
 * against the RFC's published vectors instead of only on a device.
 *
 * Nothing here logs, caches, or otherwise retains the derived bytes; the result is returned to the
 * caller and is the caller's to zero.
 */
object Scrypt {

    /**
     * Derives [ScryptParameters.derivedKeyLength] bytes from [password] and [salt].
     *
     * Memory-hard by construction: the derivation allocates 128 * N * r bytes and reads them in an
     * order that depends on the password, so an attacker cannot trade the memory away for time.
     * Callers must run this off the main thread.
     *
     * [salt] must not be empty. RFC 7914 permits an empty salt, but the platform PBKDF2 this is built
     * on does not (JCE's `PBEKeySpec` rejects it outright), so the restriction is stated here as a
     * contract rather than surfaced later as an opaque provider exception. Every caller in this app
     * passes a random per-user salt, so nothing real is given up.
     */
    suspend fun derive(
        password: ByteArray,
        salt: ByteArray,
        parameters: ScryptParameters
    ): ByteArray {
        require(salt.isNotEmpty()) { "scrypt salt must not be empty" }

        val blockBytes = 128 * parameters.r
        val expanded = pbkdf2Sha256(
            password = password,
            salt = salt,
            outputSize = blockBytes * parameters.p
        )

        val block = IntArray(blockBytes / 4)
        for (i in 0 until parameters.p) {
            readLittleEndianInts(expanded, i * blockBytes, block)
            roMix(block, parameters.n, parameters.r)
            writeLittleEndianInts(block, expanded, i * blockBytes)
        }

        return pbkdf2Sha256(
            password = password,
            salt = expanded,
            outputSize = parameters.derivedKeyLength
        )
    }

    /**
     * RFC 7914 section 5. Fills [block] with `V.get(j)`-dependent output, in place.
     *
     * The first loop writes N successive BlockMix states into V; the second reads them back at
     * indices derived from the state itself, which is the step that forces the whole of V to be kept.
     */
    private fun roMix(block: IntArray, n: Int, r: Int) {
        val blockInts = 32 * r
        val v = IntArray(n * blockInts)
        val scratch = IntArray(16)
        var x = block
        var y = IntArray(blockInts)

        for (i in 0 until n) {
            x.copyInto(v, destinationOffset = i * blockInts)
            blockMix(x, y, r, scratch)
            val swap = x; x = y; y = swap
        }

        (0 until n).forEach { _ ->
            val j = x[blockInts - 16] and (n - 1)
            val offset = j * blockInts
            for (k in 0 until blockInts) x[k] = x[k] xor v[offset + k]
            blockMix(x, y, r, scratch)
            val swap = x; x = y; y = swap
        }

        if (x !== block) x.copyInto(block)
    }

    /**
     * RFC 7914 section 4. Reads 2r sub-blocks from [input] and writes the shuffled result to [output].
     *
     * The even-indexed outputs are written to the first half and the odd-indexed to the second, which
     * is the interleaving the RFC specifies rather than an optimization.
     */
    private fun blockMix(input: IntArray, output: IntArray, r: Int, x: IntArray) {
        val subBlocks = 2 * r
        input.copyInto(
            x,
            destinationOffset = 0,
            startIndex = (subBlocks - 1) * 16,
            endIndex = subBlocks * 16
        )

        for (i in 0 until subBlocks) {
            val offset = i * 16
            for (j in 0 until 16) x[j] = x[j] xor input[offset + j]
            salsa20Core8(x)
            val destination = if (i % 2 == 0) (i / 2) * 16 else (r + i / 2) * 16
            x.copyInto(output, destinationOffset = destination)
        }
    }

    /**
     * The Salsa20/8 core, RFC 7914 section 3: eight rounds of the Salsa20 core, applied in place to
     * sixteen little-endian words.
     *
     * Written out in locals rather than over the array because this runs 2 * N * r times per
     * derivation and is the whole cost of the algorithm.
     */
    @Suppress("LongMethod")
    private fun salsa20Core8(block: IntArray) {
        var x0 = block[0]
        var x1 = block[1]
        var x2 = block[2]
        var x3 = block[3]
        var x4 = block[4]
        var x5 = block[5]
        var x6 = block[6]
        var x7 = block[7]
        var x8 = block[8]
        var x9 = block[9]
        var x10 = block[10]
        var x11 = block[11]
        var x12 = block[12]
        var x13 = block[13]
        var x14 = block[14]
        var x15 = block[15]

        repeat(4) {
            x4 = x4 xor (x0 + x12).rotateLeft(7)
            x8 = x8 xor (x4 + x0).rotateLeft(9)
            x12 = x12 xor (x8 + x4).rotateLeft(13)
            x0 = x0 xor (x12 + x8).rotateLeft(18)

            x9 = x9 xor (x5 + x1).rotateLeft(7)
            x13 = x13 xor (x9 + x5).rotateLeft(9)
            x1 = x1 xor (x13 + x9).rotateLeft(13)
            x5 = x5 xor (x1 + x13).rotateLeft(18)

            x14 = x14 xor (x10 + x6).rotateLeft(7)
            x2 = x2 xor (x14 + x10).rotateLeft(9)
            x6 = x6 xor (x2 + x14).rotateLeft(13)
            x10 = x10 xor (x6 + x2).rotateLeft(18)

            x3 = x3 xor (x15 + x11).rotateLeft(7)
            x7 = x7 xor (x3 + x15).rotateLeft(9)
            x11 = x11 xor (x7 + x3).rotateLeft(13)
            x15 = x15 xor (x11 + x7).rotateLeft(18)

            x1 = x1 xor (x0 + x3).rotateLeft(7)
            x2 = x2 xor (x1 + x0).rotateLeft(9)
            x3 = x3 xor (x2 + x1).rotateLeft(13)
            x0 = x0 xor (x3 + x2).rotateLeft(18)

            x6 = x6 xor (x5 + x4).rotateLeft(7)
            x7 = x7 xor (x6 + x5).rotateLeft(9)
            x4 = x4 xor (x7 + x6).rotateLeft(13)
            x5 = x5 xor (x4 + x7).rotateLeft(18)

            x11 = x11 xor (x10 + x9).rotateLeft(7)
            x8 = x8 xor (x11 + x10).rotateLeft(9)
            x9 = x9 xor (x8 + x11).rotateLeft(13)
            x10 = x10 xor (x9 + x8).rotateLeft(18)

            x12 = x12 xor (x15 + x14).rotateLeft(7)
            x13 = x13 xor (x12 + x15).rotateLeft(9)
            x14 = x14 xor (x13 + x12).rotateLeft(13)
            x15 = x15 xor (x14 + x13).rotateLeft(18)
        }

        block[0] += x0; block[1] += x1; block[2] += x2; block[3] += x3
        block[4] += x4; block[5] += x5; block[6] += x6; block[7] += x7
        block[8] += x8; block[9] += x9; block[10] += x10; block[11] += x11
        block[12] += x12; block[13] += x13; block[14] += x14; block[15] += x15
    }

    private suspend fun pbkdf2Sha256(
        password: ByteArray,
        salt: ByteArray,
        outputSize: Int
    ): ByteArray = CryptographyProvider.Default
        .get(PBKDF2)
        .secretDerivation(
            digest = SHA256,
            iterations = 1,
            outputSize = outputSize.bytes,
            salt = salt
        )
        .deriveSecretToByteArray(password)

    private fun readLittleEndianInts(source: ByteArray, sourceOffset: Int, destination: IntArray) {
        for (i in destination.indices) {
            val at = sourceOffset + i * 4
            destination[i] = (source[at].toInt() and 0xFF) or
                    ((source[at + 1].toInt() and 0xFF) shl 8) or
                    ((source[at + 2].toInt() and 0xFF) shl 16) or
                    ((source[at + 3].toInt() and 0xFF) shl 24)
        }
    }

    private fun writeLittleEndianInts(
        source: IntArray,
        destination: ByteArray,
        destinationOffset: Int
    ) {
        for (i in source.indices) {
            val value = source[i]
            val at = destinationOffset + i * 4
            destination[at] = value.toByte()
            destination[at + 1] = (value ushr 8).toByte()
            destination[at + 2] = (value ushr 16).toByte()
            destination[at + 3] = (value ushr 24).toByte()
        }
    }
}
