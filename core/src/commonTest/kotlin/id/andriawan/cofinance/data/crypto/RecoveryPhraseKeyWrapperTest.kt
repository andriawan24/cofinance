package id.andriawan.cofinance.data.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Exercises the phrase wrap, which is the copy the backend holds and the only route back to the
 * data on a device that holds no key material.
 */
@OptIn(ExperimentalEncodingApi::class)
class RecoveryPhraseKeyWrapperTest {

    private val wrapper = RecoveryPhraseKeyWrapper()

    @Test
    fun wrappedKeyOpensBackToTheSameKey() = runTest {
        val phrase = RecoveryPhrase.generate()
        val dataKey = DataKey.generate()

        val unwrapped = wrapper.unwrap(wrapper.wrap(dataKey, phrase), phrase)

        assertEquals(dataKey.id, unwrapped.id)
        assertContentEquals(dataKey.exportRawBytes(), unwrapped.exportRawBytes())
    }

    @Test
    fun aPhraseTypedBackInOpensTheWrap() = runTest {
        val phrase = RecoveryPhrase.generate()
        val dataKey = DataKey.generate()
        val wrap = wrapper.wrap(dataKey, phrase)

        // The restore path never holds the original object: it holds what the user typed. The
        // derivation therefore has to depend on the entropy the words encode and on nothing that
        // only the generating instance knew.
        val typedBack = assertIs<RecoveryPhraseResult.Valid>(
            RecoveryPhrase.parse("  ${phrase.text.uppercase().replace(" ", "\n")}  ")
        ).phrase

        assertContentEquals(
            dataKey.exportRawBytes(),
            wrapper.unwrap(wrap, typedBack).exportRawBytes()
        )
    }

    @Test
    fun wrapCarriesTheParametersItsUnwrapNeeds() = runTest {
        val wrap = wrapper.wrap(DataKey.generate(), RecoveryPhrase.generate())

        assertEquals(KeyWrapType.RecoveryPhrase, wrap.type)
        assertEquals(
            KeyWrapping.NONCE_SIZE,
            assertNotNull(wrap.parameter(WrappedDataKey.NONCE_PARAMETER)).size
        )
        assertEquals(
            KeyWrapping.SALT_SIZE,
            assertNotNull(wrap.parameter(WrappedDataKey.SALT_PARAMETER)).size
        )
    }

    @Test
    fun aDifferentPhraseCannotOpenTheWrap() = runTest {
        val wrap = wrapper.wrap(DataKey.generate(), RecoveryPhrase.generate())

        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap, RecoveryPhrase.generate()) }
    }

    @Test
    fun aPhraseOffByOneBitCannotOpenTheWrap() = runTest {
        val phrase = RecoveryPhrase.generate()
        val wrap = wrapper.wrap(DataKey.generate(), phrase)

        // The nearest possible wrong phrase, rather than an unrelated one: a single entropy bit
        // apart is what a user who wrote down one wrong word ends up holding, and the derivation
        // has to refuse it as flatly as it refuses a stranger's phrase.
        val neighbour = RecoveryPhrase.fromEntropy(phrase.toEntropy().also { entropy ->
            entropy[0] = (entropy[0].toInt() xor 0x01).toByte()
        })

        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap, neighbour) }
    }

    @Test
    fun tamperedSealedBytesFailAuthentication() = runTest {
        val phrase = RecoveryPhrase.generate()
        val wrap = wrapper.wrap(DataKey.generate(), phrase)

        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap.withFlippedSealedByte(), phrase) }
    }

    @Test
    fun aSubstitutedSaltFailsAuthentication() = runTest {
        val phrase = RecoveryPhrase.generate()
        val wrap = wrapper.wrap(DataKey.generate(), phrase)

        val altered = wrap.withParameter(
            WrappedDataKey.SALT_PARAMETER,
            ByteArray(KeyWrapping.SALT_SIZE) { 0 }
        )

        assertFailsWith<KeyWrapException> { wrapper.unwrap(altered, phrase) }
    }

    @Test
    fun twoWrapsOfOneKeyUnderOnePhraseShareNothingStored() = runTest {
        val phrase = RecoveryPhrase.generate()
        val dataKey = DataKey.generate()

        val first = wrapper.wrap(dataKey, phrase)
        val second = wrapper.wrap(dataKey, phrase)

        assertFalse(
            first.wrapParameters[WrappedDataKey.NONCE_PARAMETER] ==
                second.wrapParameters[WrappedDataKey.NONCE_PARAMETER],
            "Two wraps reused a nonce"
        )
        assertFalse(
            first.wrapParameters[WrappedDataKey.SALT_PARAMETER] ==
                second.wrapParameters[WrappedDataKey.SALT_PARAMETER],
            "Two wraps reused a salt"
        )
        assertFalse(first.wrappedKey == second.wrappedKey, "Two wraps produced one ciphertext")

        assertContentEquals(
            dataKey.exportRawBytes(),
            wrapper.unwrap(second, phrase).exportRawBytes()
        )
    }

    @Test
    fun theWrapNeverCarriesTheKeyOrThePhrase() = runTest {
        val phrase = RecoveryPhrase.generate()
        val dataKey = DataKey.generate()

        val encoded = Json.encodeToString(
            WrappedDataKey.serializer(),
            wrapper.wrap(dataKey, phrase)
        )
        val raw = dataKey.exportRawBytes()

        assertFalse(encoded.contains(Base64.encode(raw)), "The wrap leaked the data key")
        assertFalse(encoded.contains(Base64.UrlSafe.encode(raw)), "The wrap leaked the data key")
        assertFalse(
            encoded.contains(Base64.encode(phrase.toEntropy())),
            "The wrap leaked the phrase entropy"
        )
        // The words themselves are not searched for: the shortest BIP-0039 entries are three
        // lowercase letters and the encoded wrap is Base64, so such a match would be coincidence
        // rather than a leak. What matters is that no field can hold them, which the entropy and
        // key checks above and the field-set assertions in KeyMaterialDocumentTest establish.
    }

    @Test
    fun theUploadedKeyMaterialStillOpensAfterSerialization() = runTest {
        val phrase = RecoveryPhrase.generate()
        val dataKey = DataKey.generate()

        // The restore path reads exactly this: the uploadable subset, round-tripped through the
        // stored JSON shape and back.
        val uploaded = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(dataKey),
                wrapper.wrap(dataKey, phrase)
            )
        ).uploadableKeyMaterial()

        val restored = Json.decodeFromString(
            KeyMaterialDocument.serializer(),
            Json.encodeToString(KeyMaterialDocument.serializer(), uploaded)
        )

        val unwrapped = wrapper.unwrap(
            restored.wrapsOf(KeyWrapType.RecoveryPhrase).single(),
            phrase
        )

        assertEquals(dataKey.id, unwrapped.id)
        assertContentEquals(dataKey.exportRawBytes(), unwrapped.exportRawBytes())
    }

    @Test
    fun aWrapOfAnotherTypeIsRefusedBeforeAnythingIsDerived() = runTest {
        val deviceWrap = DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(DataKey.generate())

        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(deviceWrap, RecoveryPhrase.generate())
        }
    }

    @Test
    fun aWrapMissingItsParametersIsRefused() = runTest {
        val phrase = RecoveryPhrase.generate()
        val wrap = wrapper.wrap(DataKey.generate(), phrase)

        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(wrap.copy(wrapParameters = emptyMap()), phrase)
        }
        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(wrap.copy(wrappedKey = "not base64!!"), phrase)
        }
    }
}
