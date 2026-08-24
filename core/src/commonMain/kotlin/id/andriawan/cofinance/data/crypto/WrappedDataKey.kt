package id.andriawan.cofinance.data.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a wrapped copy of the data key is protected by.
 *
 * Stored data carries the identifier rather than an enum constant, so a copy written by a later
 * build — a second device, a rotated key — reads back as [Unrecognized] instead of failing to
 * decode. A reader that cannot open a wrap it does not understand is expected; a reader that
 * cannot read the surrounding key material because of it is not.
 */
sealed interface KeyWrapType {

    /** The value stored in [WrappedDataKey.wrapType]. */
    val id: String

    /** The non-extractable key held in Android Keystore or the iOS Keychain. Never uploaded. */
    data object Device : KeyWrapType {
        override val id: String = "device"
    }

    /** The key derived from the 12-word recovery phrase. The only wrap the backend ever holds. */
    data object RecoveryPhrase : KeyWrapType {
        override val id: String = "recovery_phrase"
    }

    /** The PIN composed with the device secret. Local only, so a PIN alone unlocks nothing. */
    data object Pin : KeyWrapType {
        override val id: String = "pin"
    }

    /** A wrap type this build does not know about, kept intact so it survives a read and rewrite. */
    data class Unrecognized(override val id: String) : KeyWrapType

    companion object {
        private val known = listOf(Device, RecoveryPhrase, Pin)

        /** Resolves a stored identifier, never throwing on one this build does not recognize. */
        fun of(id: String): KeyWrapType = known.firstOrNull { it.id == id } ?: Unrecognized(id)
    }
}

/**
 * One copy of the data encryption key, sealed by one wrap type.
 *
 * The type deliberately has no field, constructor, or accessor for the unwrapped data key: the
 * only key bytes it can hold are [wrappedKey], which are ciphertext. Unwrapping produces a
 * [DataKey] that lives in memory and is never handed back to this model.
 *
 * [wrapParameters] carries whatever the matching unwrap needs — a salt for the phrase-derived key,
 * an ephemeral public key for the device ECDH wrap, the nonce the wrap was sealed under. Keeping
 * them as a map rather than as named fields is what lets a future wrap type bring its own
 * parameters without changing this shape. Values are Base64 for the same reason
 * [EncryptedEnvelopeDocument] uses it: the store is JSON-shaped.
 */
@Serializable
data class WrappedDataKey(
    @SerialName(WRAP_TYPE_FIELD)
    val wrapType: String = "",
    @SerialName(KEY_ID_FIELD)
    val keyId: String = "",
    @SerialName(WRAPPED_KEY_FIELD)
    val wrappedKey: String = "",
    @SerialName(WRAP_PARAMETERS_FIELD)
    val wrapParameters: Map<String, String> = emptyMap()
) {

    /** The resolved wrap type, [KeyWrapType.Unrecognized] for anything this build postdates. */
    val type: KeyWrapType get() = KeyWrapType.of(wrapType)

    /**
     * Returns the sealed key bytes for the unwrapping layer.
     *
     * @throws EncryptedRecordException when the stored value is not Base64, which covers a
     * truncated or tampered key material document reaching an unwrap path.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun wrappedKeyBytes(): ByteArray = try {
        Base64.decode(wrappedKey)
    } catch (cause: IllegalArgumentException) {
        throw EncryptedRecordException("Wrapped key for $wrapType is not well-formed", cause)
    }

    /**
     * Returns the named unwrap parameter, or null when this wrap does not carry one under [name].
     *
     * @throws EncryptedRecordException when the stored value is not Base64.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun parameter(name: String): ByteArray? = wrapParameters[name]?.let { encoded ->
        try {
            Base64.decode(encoded)
        } catch (cause: IllegalArgumentException) {
            throw EncryptedRecordException("Parameter $name for $wrapType is not well-formed", cause)
        }
    }

    /** Describes the wrap without reproducing any of its bytes. */
    override fun toString(): String =
        "WrappedDataKey(wrapType=$wrapType, keyId=$keyId, parameters=${wrapParameters.keys})"

    companion object {
        const val WRAP_TYPE_FIELD = "wrap_type"
        const val KEY_ID_FIELD = "key_id"
        const val WRAPPED_KEY_FIELD = "wrapped_key"
        const val WRAP_PARAMETERS_FIELD = "wrap_parameters"

        /** The nonce the data key was sealed under, for every wrap type that seals with AES-GCM. */
        const val NONCE_PARAMETER = "nonce"

        /** The salt a derived wrapping key was stretched with, for phrase and PIN wraps. */
        const val SALT_PARAMETER = "salt"

        /** The ephemeral public key the device ECDH agreement is completed against. */
        const val EPHEMERAL_PUBLIC_KEY_PARAMETER = "ephemeral_public_key"

        /**
         * Builds a wrapped copy from the bytes a wrapping operation produced.
         *
         * There is no counterpart taking an unwrapped data key, by design: the only way into this
         * model is through bytes that have already been sealed.
         */
        @OptIn(ExperimentalEncodingApi::class)
        fun of(
            type: KeyWrapType,
            keyId: String,
            wrappedKey: ByteArray,
            parameters: Map<String, ByteArray> = emptyMap()
        ): WrappedDataKey = WrappedDataKey(
            wrapType = type.id,
            keyId = keyId,
            wrappedKey = Base64.encode(wrappedKey),
            wrapParameters = parameters.mapValues { (_, value) -> Base64.encode(value) }
        )
    }
}
