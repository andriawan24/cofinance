package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.createDeviceKeyVault
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Device test for the durability the device wrap actually depends on.
 *
 * The placeholder this replaces kept key material in a field, so every relaunch found no device
 * wrap and presented setup or restore again. That failure is invisible to a host test — an
 * in-memory fake and a real file behave identically inside one process — so the claim "the wrap
 * outlives the process" is asserted here, against the real application-private directory, by
 * writing through one store and reading through a completely fresh one whose only shared state is
 * the filesystem.
 *
 * What an instrumented test still cannot do is kill and restart the process mid-test. The stand-in
 * is [theStoredDocumentIsAFileThatOutlivesTheStoreThatWroteIt], which checks the byte-level fact a
 * process restart would rely on: the document is a file on disk under `filesDir`, and no part of
 * reading it back goes through the instance that wrote it.
 */
class AndroidLocalKeyMaterialStoreTest {

    private lateinit var dataKey: DataKey

    @Before
    fun setUp() = runBlocking {
        dataKey = DataKey.generate()
        createKeyMaterialStorage().clear()
    }

    @After
    fun tearDown() = runBlocking {
        createKeyMaterialStorage().clear()
    }

    @Test
    fun aFreshStoreReadsBackTheDeviceWrapAnEarlierStoreWrote() = runBlocking {
        val written = documentWithDeviceWrap()
        StoredLocalKeyMaterialStore(createKeyMaterialStorage()).write(written)

        // A new store over a new storage instance: nothing but the filesystem carries the document
        // from the write above to the read below.
        val reopened = StoredLocalKeyMaterialStore(createKeyMaterialStorage()).read()

        assertNotNull("the device wrap did not outlive the store that wrote it", reopened)
        assertEquals(written, reopened)
        assertEquals(1, reopened!!.wrapsOf(KeyWrapType.Device).size)
    }

    @Test
    fun theRestoredDeviceWrapStillOpensTheSameDataKey() = runBlocking {
        StoredLocalKeyMaterialStore(createKeyMaterialStorage()).write(documentWithDeviceWrap())

        val reopened = StoredLocalKeyMaterialStore(createKeyMaterialStorage()).read()
        val wrap = reopened!!.wrapsOf(KeyWrapType.Device).single()
        val unwrapped = DeviceKeyWrapper(createDeviceKeyVault()).unwrap(wrap)

        // Durability is worth nothing unless what survived is still usable against the platform key
        // material, which is the whole point of keeping the wrap rather than the key.
        assertEquals(dataKey.id, unwrapped.id)
        assertTrue(dataKey.exportRawBytes().contentEquals(unwrapped.exportRawBytes()))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun theStoredDocumentIsAFileThatOutlivesTheStoreThatWroteIt() = runBlocking {
        StoredLocalKeyMaterialStore(createKeyMaterialStorage()).write(documentWithDeviceWrap())

        val file = (createKeyMaterialStorage() as FileKeyMaterialStorage).documentFile
        assertTrue("the document was not written to app-private storage", file.isFile)
        assertTrue(file.absolutePath.contains("/files/"))
        assertTrue(file.length() > 0)

        val bytes = file.readBytes()
        val raw = dataKey.exportRawBytes()
        assertFalse(
            "the unwrapped data key was written to disk",
            bytes.decodeToString().contains(Base64.encode(raw))
        )
        assertFalse("the unwrapped data key was written to disk", bytes.containsSequence(raw))
    }

    @Test
    fun erasingRemovesTheFileRatherThanEmptyingIt() = runBlocking {
        val store = StoredLocalKeyMaterialStore(createKeyMaterialStorage())
        store.write(documentWithDeviceWrap())

        store.erase()

        assertFalse((createKeyMaterialStorage() as FileKeyMaterialStorage).documentFile.isFile)
        assertNull(StoredLocalKeyMaterialStore(createKeyMaterialStorage()).read())
    }

    private suspend fun documentWithDeviceWrap() = KeyMaterialDocument(
        keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
        wrappedKeys = listOf(DeviceKeyWrapper(createDeviceKeyVault()).wrap(dataKey))
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
