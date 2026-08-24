package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.DeviceKeyVault
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.FakeDeviceKeyVault
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The half of the durable store that is not platform-specific: encoding, decoding, and the promise
 * that nothing unwrapped goes through it.
 *
 * Durability itself cannot be asserted here — a fake byte store proves nothing about whether a file
 * or a Keychain item survives a process — so `AndroidLocalKeyMaterialStoreTest` carries that on a
 * device. What this test does carry is that a *fresh store instance* reads back what an earlier one
 * wrote while holding no state of its own between the two, which is what makes the device test's
 * stronger claim about the storage meaningful rather than accidental.
 */
class StoredLocalKeyMaterialStoreTest {

    private val storage = InMemoryKeyMaterialStorage()
    private val vault: DeviceKeyVault = FakeDeviceKeyVault()

    @Test
    fun aFreshStoreReadsBackTheDocumentAnEarlierInstanceWrote() = runTest {
        val document = documentWithDeviceAndPinWraps(DataKey.generate())
        StoredLocalKeyMaterialStore(storage).write(document)

        val reopened = StoredLocalKeyMaterialStore(storage).read()

        assertEquals(document, reopened)
        assertEquals(1, reopened?.wrapsOf(KeyWrapType.Device)?.size)
        assertEquals(1, reopened?.wrapsOf(KeyWrapType.Pin)?.size)
    }

    @Test
    fun anUnwrittenStoreHoldsNothing() = runTest {
        assertNull(StoredLocalKeyMaterialStore(storage).read())
    }

    @Test
    fun erasingRemovesTheStoredBytesRatherThanBlankingTheDocument() = runTest {
        val store = StoredLocalKeyMaterialStore(storage)
        store.write(documentWithDeviceWrap(DataKey.generate()))

        store.erase()

        assertNull(store.read())
        assertNull(storage.bytes, "erase left bytes behind for a later read to find")
    }

    @Test
    fun bytesThatNoLongerParseReadAsAnAbsentDocument() = runTest {
        storage.bytes = "{ this is not a key material document".encodeToByteArray()

        // Absent rather than thrown: a launch that cannot decode its key material has to reach
        // setup or restore, and an exception here would produce a device that cannot start at all.
        assertNull(StoredLocalKeyMaterialStore(storage).read())
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun theStoreNeverHoldsOrReturnsAnUnwrappedKey() = runTest {
        val dataKey = DataKey.generate()
        val raw = dataKey.exportRawBytes()
        val store = StoredLocalKeyMaterialStore(storage)
        store.write(documentWithDeviceAndPinWraps(dataKey))

        val stored = storage.bytes ?: error("nothing was stored")
        assertFalse(stored.containsSequence(raw), "the data key's raw bytes reached storage")
        assertFalse(
            stored.decodeToString().contains(Base64.encode(raw)),
            "the data key reached storage in encoded form"
        )

        // And on the way back out. The document type has no field an unwrapped key could occupy,
        // so this checks that the round trip did not invent one rather than checking a value.
        val reopened = store.read() ?: error("nothing was read back")
        assertTrue(reopened.wrappedKeys.isNotEmpty())
        assertFalse(
            reopened.toString().contains(Base64.encode(raw)),
            "the store's own description carries key material"
        )
    }

    private suspend fun documentWithDeviceWrap(dataKey: DataKey) = KeyMaterialDocument(
        keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
        wrappedKeys = listOf(DeviceKeyWrapper(vault).wrap(dataKey))
    )

    private suspend fun documentWithDeviceAndPinWraps(dataKey: DataKey) = KeyMaterialDocument(
        keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
        wrappedKeys = listOf(
            DeviceKeyWrapper(vault).wrap(dataKey),
            PinKeyWrapper(vault).wrap(dataKey, "123456")
        )
    )

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

/** A [KeyMaterialStorage] holding one blob, standing in for a file or a Keychain item. */
class InMemoryKeyMaterialStorage(var bytes: ByteArray? = null) : KeyMaterialStorage {

    override suspend fun read(): ByteArray? = bytes

    override suspend fun write(bytes: ByteArray) {
        this.bytes = bytes
    }

    override suspend fun clear() {
        bytes = null
    }
}
