package id.andriawan.cofinance.data.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

/**
 * Reproduces the scrypt test vectors published in
 * [RFC 7914 section 12](https://www.rfc-editor.org/rfc/rfc7914#section-12).
 *
 * These vectors are the only evidence that the common-code implementation is scrypt and not merely
 * something self-consistent, so they are transcribed verbatim from the RFC rather than captured from
 * this implementation's own output.
 */
class ScryptTest {

    // RFC 7914's first vector (P="", S="", N=16, r=1, p=1) is not reproducible here. It derives from
    // an empty salt, and the platform PBKDF2 that Decision 14 builds scrypt on rejects that:
    // JCE's PBEKeySpec throws "the salt parameter must not be empty" before any scrypt code runs.
    // Scrypt.derive states the non-empty-salt contract instead, asserted by emptySaltIsRejected below.
    // The vector exercises no code path that the second and third vectors leave uncovered.

    @Test
    fun rfc7914SecondVector() = runTest {
        // scrypt (P="password", S="NaCl", N=1024, r=8, p=16, dkLen=64)
        assertDerives(
            password = "password",
            salt = "NaCl",
            parameters = ScryptParameters(n = 1024, r = 8, p = 16, derivedKeyLength = 64),
            expected = """
                fd ba be 1c 9d 34 72 00 78 56 e7 19 0d 01 e9 fe
                7c 6a d7 cb c8 23 78 30 e7 73 76 63 4b 37 31 62
                2e af 30 d9 2e 22 a3 88 6f f1 09 27 9d 98 30 da
                c7 27 af b9 4a 83 ee 6d 83 60 cb df a2 cc 06 40
            """
        )
    }

    @Test
    fun rfc7914ThirdVector() = runTest {
        // scrypt (P="pleaseletmein", S="SodiumChloride", N=16384, r=8, p=1, dkLen=64).
        // This is the app's own N and r, so this vector also proves ScryptParameters.PIN is viable.
        assertDerives(
            password = "pleaseletmein",
            salt = "SodiumChloride",
            parameters = ScryptParameters(n = 16384, r = 8, p = 1, derivedKeyLength = 64),
            expected = """
                70 23 bd cb 3a fd 73 48 46 1c 06 cd 81 fd 38 eb
                fd a8 fb ba 90 4f 8e 3e a9 b5 43 f6 54 5d a1 f2
                d5 43 29 55 61 3f 0f cf 62 d4 97 05 24 2a 9a f9
                e6 1e 85 dc 0d 65 1e 40 df cf 01 7b 45 57 58 87
            """
        )
    }

    // RFC 7914's fourth vector (N=1048576, r=8, p=1) is deliberately omitted: it needs a 1 GiB
    // working set, which exceeds the test JVM's heap. The third vector exercises the identical code
    // path at the parameters this app actually ships.

    @Test
    fun pinParametersDeriveAnAesSizedKey() = runTest {
        val derived = Scrypt.derive(
            password = "483920".encodeToByteArray(),
            salt = ByteArray(16) { it.toByte() },
            parameters = ScryptParameters.PIN
        )

        assertEquals(32, derived.size)
    }

    @Test
    fun differentPinsDeriveDifferentKeys() = runTest {
        val salt = ByteArray(16) { it.toByte() }
        val first = Scrypt.derive("483920".encodeToByteArray(), salt, ScryptParameters.PIN)
        val second = Scrypt.derive("483921".encodeToByteArray(), salt, ScryptParameters.PIN)

        assertFalse(first.contentEquals(second), "A different PIN derived the same key")
    }

    @Test
    fun differentSaltsDeriveDifferentKeys() = runTest {
        val pin = "483920".encodeToByteArray()
        val first = Scrypt.derive(pin, ByteArray(16) { it.toByte() }, ScryptParameters.PIN)
        val second = Scrypt.derive(pin, ByteArray(16) { (it + 1).toByte() }, ScryptParameters.PIN)

        assertFalse(first.contentEquals(second), "A different salt derived the same key")
    }

    @Test
    fun emptySaltIsRejected() = runTest {
        assertFailsWith<IllegalArgumentException> {
            Scrypt.derive("483920".encodeToByteArray(), ByteArray(0), ScryptParameters.PIN)
        }
    }

    @Test
    fun costParameterThatIsNotAPowerOfTwoIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 1000, r = 8, p = 1, derivedKeyLength = 32)
        }
    }

    @Test
    fun costParameterOfOneIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 1, r = 8, p = 1, derivedKeyLength = 32)
        }
    }

    @Test
    fun nonPositiveBlockSizeAndParallelizationAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 16, r = 0, p = 1, derivedKeyLength = 32)
        }
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 16, r = 8, p = 0, derivedKeyLength = 32)
        }
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 16, r = 8, p = 1, derivedKeyLength = 0)
        }
    }

    @Test
    fun blockSizesThatWouldOverflowAreRejected() {
        // 128 * r * p exceeds Int.MAX_VALUE, which would wrap to a negative allocation size.
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 16, r = 1 shl 25, p = 1 shl 5, derivedKeyLength = 32)
        }
        // 32 * N * r exceeds Int.MAX_VALUE, which would wrap the ROMix array size.
        assertFailsWith<IllegalArgumentException> {
            ScryptParameters(n = 1 shl 26, r = 1 shl 20, p = 1, derivedKeyLength = 32)
        }
    }

    private suspend fun assertDerives(
        password: String,
        salt: String,
        parameters: ScryptParameters,
        expected: String
    ) {
        val derived = Scrypt.derive(
            password = password.encodeToByteArray(),
            salt = salt.encodeToByteArray(),
            parameters = parameters
        )

        assertEquals(expected.filterNot(Char::isWhitespace), derived.toHex())
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        val value = byte.toInt() and 0xFF
        HEX[value shr 4].toString() + HEX[value and 0x0F]
    }

    private companion object {
        const val HEX = "0123456789abcdef"
    }
}
