package id.andriawan.cofinance.data.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The stored copy of the phrase that makes re-display possible.
 *
 * The claim being tested is narrow and is the whole reason the vault can live in ordinary storage:
 * the blob is worth nothing without the data key. So the interesting cases are the ones where the
 * key is wrong or the bytes have been edited, and both must produce nothing rather than something.
 */
class RecoveryPhraseVaultTest {

    private var stored: String? = null

    private val vault = SealedRecoveryPhraseVault(
        readSealed = { stored },
        writeSealed = { stored = it },
        clearSealed = { stored = null }
    )

    @Test
    fun theStoredPhraseIsReturnedToAHolderOfTheDataKey() = runTest {
        val dataKey = DataKey.generate()
        val phrase = RecoveryPhrase.generate()

        vault.store(phrase, dataKey)

        assertEquals(phrase.words, vault.read(dataKey)?.words)
    }

    @Test
    fun aDifferentDataKeyOpensNothing() = runTest {
        vault.store(RecoveryPhrase.generate(), DataKey.generate())

        assertNull(vault.read(DataKey.generate()))
    }

    @Test
    fun anEditedBlobOpensNothing() = runTest {
        val dataKey = DataKey.generate()
        vault.store(RecoveryPhrase.generate(), dataKey)

        // Flipping one stored character is the cheapest stand-in for a tampered file; AES-GCM
        // authenticates, so the result must be nothing rather than different words.
        val original = requireNotNull(stored)
        stored = original.take(original.length - 2) +
            (if (original[original.length - 2] == 'A') 'B' else 'A') +
            original.last()

        assertNull(vault.read(dataKey))
    }

    @Test
    fun nothingIsStoredBeforeAPhraseIsKept() = runTest {
        assertNull(vault.read(DataKey.generate()))
    }

    @Test
    fun erasingRemovesTheStoredCopy() = runTest {
        val dataKey = DataKey.generate()
        vault.store(RecoveryPhrase.generate(), dataKey)

        vault.erase()

        assertNull(stored)
        assertNull(vault.read(dataKey))
    }

    @Test
    fun twoStoresOfOnePhraseUnderOneKeyProduceDifferentBytes() = runTest {
        val dataKey = DataKey.generate()
        val phrase = RecoveryPhrase.generate()

        vault.store(phrase, dataKey)
        val first = requireNotNull(stored)
        vault.store(phrase, dataKey)
        val second = requireNotNull(stored)

        assertNotEquals(first, second, "A repeated GCM nonce under one key would be catastrophic")
        assertEquals(phrase.words, vault.read(dataKey)?.words)
    }

    @Test
    fun theStoredBlobDoesNotContainThePhrase() = runTest {
        val dataKey = DataKey.generate()
        val phrase = RecoveryPhrase.generate()

        vault.store(phrase, dataKey)

        val blob = requireNotNull(stored)
        assertTrue(phrase.words.none { word -> blob.contains(word) })
    }
}
