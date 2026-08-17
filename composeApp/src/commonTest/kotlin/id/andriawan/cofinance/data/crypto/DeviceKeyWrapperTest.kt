package id.andriawan.cofinance.data.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Exercises the device wrap through [FakeDeviceKeyVault], which is the only vault common tests can
 * reach. What is under test is the ECDH-plus-HKDF composition and the seal above it; hardware
 * backing is a platform property asserted by the checks in tasks 2.3 and 2.4.
 */
@OptIn(ExperimentalEncodingApi::class)
class DeviceKeyWrapperTest {

    @Test
    fun wrappedKeyOpensBackToTheSameKey() = runTest {
        val wrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
        val dataKey = DataKey.generate()

        val unwrapped = wrapper.unwrap(wrapper.wrap(dataKey))

        assertEquals(dataKey.id, unwrapped.id)
        assertContentEquals(dataKey.exportRawBytes(), unwrapped.exportRawBytes())
    }

    @Test
    fun wrapCarriesTheParametersItsUnwrapNeeds() = runTest {
        val wrap = DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(DataKey.generate())

        assertEquals(KeyWrapType.Device, wrap.type)
        assertEquals(
            KeyWrapping.NONCE_SIZE,
            assertNotNull(wrap.parameter(WrappedDataKey.NONCE_PARAMETER)).size
        )
        assertEquals(
            DeviceKeyVault.PUBLIC_KEY_SIZE,
            assertNotNull(wrap.parameter(WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER)).size
        )
    }

    @Test
    fun aDifferentDeviceKeyCannotOpenTheWrap() = runTest {
        val wrap = DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(DataKey.generate())
        val otherDevice = DeviceKeyWrapper(FakeDeviceKeyVault())

        assertFailsWith<KeyWrapException> { otherDevice.unwrap(wrap) }
    }

    @Test
    fun aDeviceThatDestroyedItsKeyMaterialCannotOpenTheWrap() = runTest {
        val vault = FakeDeviceKeyVault()
        val wrapper = DeviceKeyWrapper(vault)
        val wrap = wrapper.wrap(DataKey.generate())

        // Destruction is what the tenth failed PIN attempt performs. Recovery is then the phrase
        // wrap's job, and this copy has to be genuinely closed rather than merely unreachable.
        vault.destroyKeyMaterial()

        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap) }
    }

    @Test
    fun tamperedSealedBytesFailAuthentication() = runTest {
        val wrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
        val wrap = wrapper.wrap(DataKey.generate())

        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap.withFlippedSealedByte()) }
    }

    @Test
    fun tamperedNonceFailsAuthentication() = runTest {
        val wrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
        val wrap = wrapper.wrap(DataKey.generate())

        val altered = wrap.withParameter(
            WrappedDataKey.NONCE_PARAMETER,
            ByteArray(KeyWrapping.NONCE_SIZE) { 0 }
        )

        assertFailsWith<KeyWrapException> { wrapper.unwrap(altered) }
    }

    @Test
    fun aSubstitutedEphemeralKeyFailsAuthentication() = runTest {
        val vault = FakeDeviceKeyVault()
        val wrapper = DeviceKeyWrapper(vault)
        val dataKey = DataKey.generate()
        val wrap = wrapper.wrap(dataKey)

        // The ephemeral public key is unauthenticated storage, so an attacker can replace it with
        // one they hold. Doing so changes the agreed secret and therefore the wrapping key, which
        // is the property that stops it from being a way in.
        val theirEphemeral = FakeDeviceKeyVault().devicePublicKey()
        val altered =
            wrap.withParameter(WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER, theirEphemeral)

        assertFailsWith<KeyWrapException> { wrapper.unwrap(altered) }
    }

    @Test
    fun twoWrapsOfOneKeyShareNoNonceAndNoCiphertext() = runTest {
        val wrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
        val dataKey = DataKey.generate()

        val first = wrapper.wrap(dataKey)
        val second = wrapper.wrap(dataKey)

        assertFalse(
            first.wrapParameters[WrappedDataKey.NONCE_PARAMETER] ==
                second.wrapParameters[WrappedDataKey.NONCE_PARAMETER],
            "Two wraps reused a nonce"
        )
        assertFalse(
            first.wrapParameters[WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER] ==
                second.wrapParameters[WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER],
            "Two wraps reused an ephemeral key pair"
        )
        assertFalse(first.wrappedKey == second.wrappedKey, "Two wraps produced one ciphertext")

        assertContentEquals(
            dataKey.exportRawBytes(),
            wrapper.unwrap(second).exportRawBytes()
        )
    }

    @Test
    fun theWrapNeverCarriesTheKeyItProtects() = runTest {
        val dataKey = DataKey.generate()
        val wrap = DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(dataKey)

        val encoded = Json.encodeToString(WrappedDataKey.serializer(), wrap)
        val raw = dataKey.exportRawBytes()

        assertFalse(encoded.contains(Base64.encode(raw)), "The wrap leaked the data key")
        assertFalse(encoded.contains(Base64.UrlSafe.encode(raw)), "The wrap leaked the data key")
    }

    @Test
    fun aWrapStoredInKeyMaterialStillOpensAfterSerialization() = runTest {
        val wrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
        val dataKey = DataKey.generate()

        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(wrapper.wrap(dataKey))
        )
        val restored = Json.decodeFromString(
            KeyMaterialDocument.serializer(),
            Json.encodeToString(KeyMaterialDocument.serializer(), document)
        )

        val unwrapped = wrapper.unwrap(restored.wrapsOf(KeyWrapType.Device).single())

        assertEquals(dataKey.id, unwrapped.id)
        assertContentEquals(dataKey.exportRawBytes(), unwrapped.exportRawBytes())
    }

    @Test
    fun aWrapOfAnotherTypeIsRefusedBeforeAnythingIsDerived() = runTest {
        val phraseWrap = RecoveryPhraseKeyWrapper()
            .wrap(DataKey.generate(), RecoveryPhrase.generate())

        assertFailsWith<KeyWrapException> {
            DeviceKeyWrapper(FakeDeviceKeyVault()).unwrap(phraseWrap)
        }
    }

    @Test
    fun aWrapMissingItsParametersIsRefused() = runTest {
        val wrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
        val wrap = wrapper.wrap(DataKey.generate())

        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(wrap.copy(wrapParameters = emptyMap()))
        }
        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(
                wrap.copy(
                    wrapParameters = wrap.wrapParameters +
                        (WrappedDataKey.NONCE_PARAMETER to "not base64!!")
                )
            )
        }
    }
}

/** Replaces one stored parameter, standing in for an edit made to the stored document. */
@OptIn(ExperimentalEncodingApi::class)
internal fun WrappedDataKey.withParameter(name: String, value: ByteArray): WrappedDataKey =
    copy(wrapParameters = wrapParameters + (name to Base64.encode(value)))

/** Flips a bit in the sealed bytes, standing in for an edit made to the stored ciphertext. */
@OptIn(ExperimentalEncodingApi::class)
internal fun WrappedDataKey.withFlippedSealedByte(): WrappedDataKey {
    val sealed = wrappedKeyBytes()
    sealed[0] = (sealed[0].toInt() xor 0x01).toByte()
    return copy(wrappedKey = Base64.encode(sealed))
}
