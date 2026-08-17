package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Exercises the PIN wrap, the copy the unlock screen opens.
 *
 * Two properties are what this file is really for, and neither is about round-tripping: that the
 * wrap is opened by deriving from the PIN rather than by comparing against something stored, and
 * that the correct PIN is worthless without the device secret it is composed with. A six-digit PIN
 * is 10^6 values, so the device binding is the only thing standing between an attacker holding
 * stored material and the data key.
 *
 * Every wrap and every unwrap here costs one scrypt derivation at N=16384, so the case count is
 * kept deliberately small.
 */
@OptIn(ExperimentalEncodingApi::class)
class PinKeyWrapperTest {

    @Test
    fun wrappedKeyOpensBackToTheSameKeyOnTheSameDevice() = runTest {
        val wrapper = PinKeyWrapper(FakeDeviceKeyVault())
        val dataKey = DataKey.generate()

        val wrap = wrapper.wrap(dataKey, PIN)
        val unwrapped = wrapper.unwrap(wrap, PIN)

        assertEquals(KeyWrapType.Pin, wrap.type)
        assertEquals(dataKey.id, unwrapped.id)
        assertContentEquals(dataKey.exportRawBytes(), unwrapped.exportRawBytes())
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
    fun theCorrectPinUnderADifferentDeviceSecretYieldsNothing() = runTest {
        // The scenario the device binding exists for: stored material and the correct PIN, carried
        // to a device that holds a different secret. Both vaults are the same implementation and
        // differ only in the secret they hold, so nothing but the composition can be refusing this.
        val wrap = PinKeyWrapper(FakeDeviceKeyVault()).wrap(DataKey.generate(), PIN)

        assertFailsWith<KeyWrapException> {
            PinKeyWrapper(FakeDeviceKeyVault()).unwrap(wrap, PIN)
        }
    }

    @Test
    fun aDifferentPinCannotOpenTheWrap() = runTest {
        val wrapper = PinKeyWrapper(FakeDeviceKeyVault())
        val wrap = wrapper.wrap(DataKey.generate(), PIN)

        // One digit apart, which is what a mistyped PIN actually looks like, and then an empty
        // entry, which is what a UI bug submits.
        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap, "123457") }
        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap, "") }
    }

    @Test
    fun editedStoredBytesFailAuthentication() = runTest {
        val wrapper = PinKeyWrapper(FakeDeviceKeyVault())
        val wrap = wrapper.wrap(DataKey.generate(), PIN)

        assertFailsWith<KeyWrapException> { wrapper.unwrap(wrap.withFlippedSealedByte(), PIN) }
        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(
                wrap.withParameter(WrappedDataKey.SALT_PARAMETER, ByteArray(KeyWrapping.SALT_SIZE)),
                PIN
            )
        }
    }

    @Test
    fun twoWrapsOfOneKeyUnderOnePinShareNothingStored() = runTest {
        val wrapper = PinKeyWrapper(FakeDeviceKeyVault())
        val dataKey = DataKey.generate()

        val first = wrapper.wrap(dataKey, PIN)
        val second = wrapper.wrap(dataKey, PIN)

        assertNotEquals(
            first.wrapParameters[WrappedDataKey.SALT_PARAMETER],
            second.wrapParameters[WrappedDataKey.SALT_PARAMETER],
            "Two wraps reused a salt"
        )
        assertNotEquals(
            first.wrapParameters[WrappedDataKey.NONCE_PARAMETER],
            second.wrapParameters[WrappedDataKey.NONCE_PARAMETER],
            "Two wraps reused a nonce"
        )
        assertNotEquals(first.wrappedKey, second.wrappedKey, "Two wraps produced one ciphertext")
    }

    @Test
    fun nothingStoredCouldServeAsAPinVerifier() = runTest {
        val vault = FakeDeviceKeyVault()
        val dataKey = DataKey.generate()
        // The salt and nonce are held fixed across the four wraps below, so the only thing that can
        // move the stored bytes is the derivation itself.
        val wrap = PinKeyWrapper(vault, fixedRandom()).wrap(dataKey, PIN)

        // Every stored byte is accounted for: the two derivation parameters, and a ciphertext the
        // exact size of the data key plus a GCM tag. There is no field, and no spare byte, in which
        // a hash of the PIN or any other value a comparison could be made against would fit.
        assertEquals(
            setOf(WrappedDataKey.NONCE_PARAMETER, WrappedDataKey.SALT_PARAMETER),
            wrap.wrapParameters.keys
        )
        assertEquals(dataKey.exportRawBytes().size + GCM_TAG_BYTES, wrap.wrappedKeyBytes().size)

        // The sealed bytes are a function of both secrets jointly. With the salt and the nonce held
        // identical across all three wraps — asserted, so no difference below can be attributed to
        // them — changing the PIN alone or the device secret alone changes the ciphertext. The key
        // is therefore sealed under something derived from both, rather than under one of them with
        // the other checked alongside it, which is the arrangement Decision 3 rejects.
        val underAnotherPin = PinKeyWrapper(vault, fixedRandom()).wrap(dataKey, OTHER_PIN)
        val underAnotherDevice = PinKeyWrapper(FakeDeviceKeyVault(), fixedRandom()).wrap(dataKey, PIN)

        assertEquals(wrap.wrapParameters, underAnotherPin.wrapParameters)
        assertEquals(wrap.wrapParameters, underAnotherDevice.wrapParameters)
        assertNotEquals(
            wrap.wrappedKey,
            underAnotherPin.wrappedKey,
            "The sealed bytes did not depend on the PIN"
        )
        assertNotEquals(
            wrap.wrappedKey,
            underAnotherDevice.wrappedKey,
            "The sealed bytes did not depend on the device secret"
        )

        // And opening it is reproduction, not recall: a wrapper that never produced this wrap and
        // holds no state from the one that did opens it from the PIN, the stored salt, and the
        // vault alone. GCM authenticating is what makes that meaningful — it means the wrapping key
        // was rebuilt byte for byte from those three inputs, so the derivation is the access path
        // and there is nothing left for a comparison to gate.
        assertContentEquals(
            dataKey.exportRawBytes(),
            PinKeyWrapper(vault).unwrap(wrap, PIN).exportRawBytes()
        )

        // And the stored form carries neither of the two secrets it was derived from.
        val encoded = Json.encodeToString(WrappedDataKey.serializer(), wrap)
        assertFalse(
            encoded.contains(Base64.encode(dataKey.exportRawBytes())),
            "The wrap leaked the data key"
        )
        assertFalse(
            encoded.contains(Base64.encode(vault.deviceSecret())),
            "The wrap leaked the device secret"
        )
        // The PIN digits themselves are not searched for, for the reason RecoveryPhraseKeyWrapperTest
        // gives about phrase words: the encoded wrap is Base64 over an alphabet that includes
        // digits, so such a match would be coincidence rather than a leak. What matters is that no
        // field can hold them, which the field-set assertion above establishes.
    }

    @Test
    fun aPinWrapIsNeverPartOfTheUploadedKeyMaterial() = runTest {
        val dataKey = DataKey.generate()
        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(dataKey),
                RecoveryPhraseKeyWrapper().wrap(dataKey, RecoveryPhrase.generate()),
                PinKeyWrapper(FakeDeviceKeyVault()).wrap(dataKey, PIN)
            )
        )

        val uploadable = document.uploadableKeyMaterial()

        // Asserted here as well as in KeyMaterialDocumentTest, and deliberately: this is the whole
        // reason a six-digit secret is safe to use at all, so a later change to the upload filter
        // has to fail this file too, not only the document's own tests.
        assertEquals(emptyList(), uploadable.wrapsOf(KeyWrapType.Pin))
        assertFalse(
            Json.encodeToString(KeyMaterialDocument.serializer(), uploadable)
                .contains(KeyWrapType.Pin.id),
            "The uploaded key material mentions the PIN wrap"
        )
        assertEquals(1, uploadable.wrappedKeys.size)
        assertEquals(KeyWrapType.RecoveryPhrase, uploadable.wrappedKeys.single().type)
    }

    @Test
    fun aWrapOfAnotherTypeIsRefusedBeforeAnythingIsDerived() = runTest {
        val deviceWrap = DeviceKeyWrapper(FakeDeviceKeyVault()).wrap(DataKey.generate())

        assertFailsWith<KeyWrapException> {
            PinKeyWrapper(FakeDeviceKeyVault()).unwrap(deviceWrap, PIN)
        }
    }

    @Test
    fun aWrapMissingItsParametersIsRefused() = runTest {
        val wrapper = PinKeyWrapper(FakeDeviceKeyVault())
        val wrap = wrapper.wrap(DataKey.generate(), PIN)

        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(wrap.copy(wrapParameters = emptyMap()), PIN)
        }
        assertFailsWith<KeyWrapException> {
            wrapper.unwrap(wrap.copy(wrappedKey = "not base64!!"), PIN)
        }
    }

    private companion object {
        const val PIN = "123456"
        const val OTHER_PIN = "654321"

        /** AES-GCM's authentication tag, appended to the sealed key bytes. */
        const val GCM_TAG_BYTES = 16

        /**
         * A stand-in for [CryptographyRandom] that draws the same salt and nonce every time.
         *
         * Only for holding the stored parameters still while the derivation inputs are varied. It
         * is emphatically not what the wrappers do in production, where a repeated nonce under GCM
         * would be catastrophic — [twoWrapsOfOneKeyUnderOnePinShareNothingStored] is what asserts
         * the real behavior.
         */
        fun fixedRandom(): CryptographyRandom = object : CryptographyRandom() {
            private val source = Random(seed = 20260817)
            override fun nextBits(bitCount: Int): Int = source.nextBits(bitCount)
        }
    }
}
