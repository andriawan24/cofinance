package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.AndroidDeviceKeyVault
import id.andriawan.cofinance.data.crypto.DeviceKeyVaultStorage
import id.andriawan.cofinance.data.crypto.createDeviceKeyVault
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.KeyStore

/**
 * Device test for the Android failed-attempt counter.
 *
 * The properties under test are about *where* the counter lives, and none of them is observable
 * from a host JVM: the Android Keystore has no host implementation, so whether the sealing key is
 * non-extractable, and whether the record survives a fresh instance, can only be asked here.
 *
 * ## What this test can and cannot say about reinstall persistence
 *
 * The requirement is that reinstalling the app does not reset the counter. An instrumented test
 * runs inside one installation, so it cannot uninstall the app and come back; that step stays
 * manually verifiable only. What it can establish is the mechanism the guarantee rests on, and
 * that is what [theCounterAndTheDeviceKeyMaterialDieTogether] does: the counter is sealed by a
 * Keystore key, and the event that removes it — clearing app data or uninstalling, both of which
 * delete this application's Keystore entries — is the same event that removes the device key
 * material the PIN wrap is derived from. A reinstall therefore returns to a device that has no
 * openable PIN wrap at all, rather than to a fresh set of attempts against the same wrapped key.
 */
class AndroidFailedAttemptStoreTest {

    private lateinit var store: KeystoreSealedFailedAttemptStore

    @Before
    fun setUp() = runBlocking {
        store = createFailedAttemptStore() as KeystoreSealedFailedAttemptStore
        store.clear()
        deleteSealingKey()
    }

    @After
    fun tearDown() = runBlocking {
        store.clear()
        deleteSealingKey()
    }

    @Test
    fun anAbsentCounterReadsAsNoneRatherThanAsTampering() = runBlocking {
        assertEquals(StoredFailedAttempts.None, store.read())
    }

    @Test
    fun theCounterSurvivesAFreshInstanceOfTheStore() = runBlocking {
        store.write(FailedAttemptRecord(consecutiveFailures = 7, lastFailureAtMillis = 1_234_567))

        // A second instance is what a relaunched process gets: nothing is cached in the object.
        val relaunched = createFailedAttemptStore()
        val stored = relaunched.read()

        assertTrue("expected a record, got $stored", stored is StoredFailedAttempts.Recorded)
        val record = (stored as StoredFailedAttempts.Recorded).record
        assertEquals(7, record.consecutiveFailures)
        assertEquals(1_234_567L, record.lastFailureAtMillis)
    }

    @Test
    fun theSealingKeyIsKeystoreResidentAndCannotBeExported() = runBlocking {
        store.write(FailedAttemptRecord(1, 1))

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey(KeystoreSealedFailedAttemptStore.KEY_ALIAS, null)

        assertNotNull("no sealing key was created for the counter", key)
        assertNull("the counter's sealing key material escaped the keystore", key.encoded)
    }

    @Test
    fun theStoredRecordIsCiphertextRatherThanAReadableNumber() = runBlocking {
        val record = FailedAttemptRecord(consecutiveFailures = 9, lastFailureAtMillis = 0x0102030405)
        store.write(record)
        val first = recordFile().readBytes()
        store.write(record)
        val second = recordFile().readBytes()

        assertFalse("the record was stored in the clear", first.containsSequence(encode(record)))
        // A fresh nonce per write, so two writes of the same value are not byte-identical and the
        // count cannot be read off by comparing files.
        assertFalse("the seal is deterministic", first.contentEquals(second))
    }

    @Test
    fun anEditedRecordReadsAsUnreadableRatherThanAsANumber() = runBlocking {
        store.write(FailedAttemptRecord(9, 1))

        val file = recordFile()
        val bytes = file.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()
        file.writeBytes(bytes)

        assertEquals(StoredFailedAttempts.Unreadable, store.read())
    }

    @Test
    fun aRecordSeparatedFromItsKeyReadsAsUnreadable() = runBlocking {
        store.write(FailedAttemptRecord(9, 1))

        // What a restored backup looks like: app-private files present, Keystore entries absent.
        deleteSealingKey()

        assertEquals(StoredFailedAttempts.Unreadable, store.read())
    }

    /**
     * The coupling the reinstall guarantee rests on.
     *
     * Deleting this application's Keystore entries is what clearing app data and uninstalling both
     * do. Afterwards the counter no longer opens *and* the device secret is a different value, so
     * the PIN wrap that the counter was protecting cannot be derived by any PIN.
     */
    @Test
    fun theCounterAndTheDeviceKeyMaterialDieTogether() = runBlocking {
        val vault = createDeviceKeyVault() as AndroidDeviceKeyVault
        val secretBefore = vault.deviceSecret()
        store.write(FailedAttemptRecord(9, 1))

        deleteSealingKey()
        vault.destroyKeyMaterial()

        assertEquals(StoredFailedAttempts.Unreadable, store.read())
        val secretAfter = vault.deviceSecret()
        assertFalse(
            "the device secret survived, so the PIN wrap would still be derivable",
            secretBefore.contentEquals(secretAfter)
        )
        vault.destroyKeyMaterial()
    }

    @Test
    fun clearingRemovesTheRecordAndAWriteRebuildsIt() = runBlocking {
        store.write(FailedAttemptRecord(4, 99))
        store.clear()
        assertEquals(StoredFailedAttempts.None, store.read())

        store.write(FailedAttemptRecord(1, 100))
        val stored = store.read()
        assertTrue(stored is StoredFailedAttempts.Recorded)
        assertEquals(1, (stored as StoredFailedAttempts.Recorded).record.consecutiveFailures)
    }

    @Test
    fun theEncodedRecordRoundTripsExactly() = runBlocking {
        val record = FailedAttemptRecord(
            consecutiveFailures = Int.MAX_VALUE,
            lastFailureAtMillis = Long.MAX_VALUE
        )
        store.write(record)

        val stored = store.read()
        assertTrue(stored is StoredFailedAttempts.Recorded)
        assertArrayEquals(
            encode(record),
            encode((stored as StoredFailedAttempts.Recorded).record)
        )
    }

    private fun recordFile(): File = File(
        File(DeviceKeyVaultStorage.applicationContext!!.filesDir, "app-lock"),
        KeystoreSealedFailedAttemptStore.RECORD_FILE
    )

    private fun deleteSealingKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KeystoreSealedFailedAttemptStore.KEY_ALIAS)) {
            keyStore.deleteEntry(KeystoreSealedFailedAttemptStore.KEY_ALIAS)
        }
    }

    private fun encode(record: FailedAttemptRecord): ByteArray {
        val bytes = ByteArray(12)
        var index = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            bytes[index++] = (record.consecutiveFailures ushr shift).toByte()
        }
        for (shift in intArrayOf(56, 48, 40, 32, 24, 16, 8, 0)) {
            bytes[index++] = (record.lastFailureAtMillis ushr shift).toByte()
        }
        return bytes
    }

    private fun ByteArray.containsSequence(needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > size) return false
        outer@ for (start in 0..size - needle.size) {
            for (index in needle.indices) {
                if (this[start + index] != needle[index]) continue@outer
            }
            return true
        }
        return false
    }
}
