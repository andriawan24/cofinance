package id.andriawan.cofinance.data.crypto

import id.andriawan.cofinance.data.model.response.AccountResponse
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

/**
 * The synchronization coordinator re-encrypts the entire local snapshot on every mirror, so identical
 * input is sealed over and over under the same key. A nonce derived from record content or from a
 * counter would repeat across those runs, and nonce reuse under AES-GCM is catastrophic rather than
 * degrading. These tests exist to keep that a correctness property rather than an intention.
 */
class NonceUniquenessTest {

    private val cipher = RecordCipher()

    private val account = AccountResponse(
        id = "account-1",
        name = "BCA",
        balance = 12_450_000L,
        createdAt = "2026-08-17T09:00:00Z"
    )

    @Test
    fun identicalInputSealedTwiceUsesDifferentNonces() = runTest {
        val key = DataKey.generate()

        val first = cipher.seal(account, AccountResponse.serializer(), key)
        val second = cipher.seal(account, AccountResponse.serializer(), key)

        assertFalse(
            first.nonce.contentEquals(second.nonce),
            "Nonce repeated for identical input under the same key"
        )
        assertFalse(
            first.ciphertext.contentEquals(second.ciphertext),
            "Ciphertext repeated for identical input, so the nonce was not applied"
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun repeatedSealsOfIdenticalInputNeverRepeatANonce() = runTest {
        val key = DataKey.generate()
        val seen = mutableSetOf<String>()

        repeat(SAMPLE_SIZE) {
            val envelope = cipher.seal(account, AccountResponse.serializer(), key)
            seen += Base64.encode(envelope.nonce)
        }

        assertEquals(SAMPLE_SIZE, seen.size, "A nonce repeated across $SAMPLE_SIZE seals")
    }

    @Test
    fun everyNonceIsFullWidth() = runTest {
        val key = DataKey.generate()

        repeat(16) {
            assertEquals(
                EncryptedEnvelope.NONCE_SIZE,
                cipher.seal(account, AccountResponse.serializer(), key).nonce.size
            )
        }
    }

    private companion object {
        const val SAMPLE_SIZE = 256
    }
}
