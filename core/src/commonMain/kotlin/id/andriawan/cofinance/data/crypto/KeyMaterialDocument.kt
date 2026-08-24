package id.andriawan.cofinance.data.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The stored key material: every wrapped copy of the data key, and nothing else.
 *
 * [wrappedKeys] is a list rather than one field per wrap type even though only two copies exist at
 * launch. This is the whole reason a future multi-device change is additive: pairing a second
 * device appends a wrapped copy, and no existing record is re-encrypted.
 *
 * The unwrapped data key has no representation here — see [WrappedDataKey] — so no code path can
 * store or transmit it by handling this document.
 */
@Serializable
data class KeyMaterialDocument(
    @SerialName(VERSION_FIELD)
    val keyMaterialVersion: Int = 0,
    @SerialName(WRAPPED_KEYS_FIELD)
    val wrappedKeys: List<WrappedDataKey> = emptyList()
) {

    /** The wrapped copies protected by [type], of which there may be more than one per type. */
    fun wrapsOf(type: KeyWrapType): List<WrappedDataKey> = wrappedKeys.filter { it.type == type }

    /**
     * Returns the subset of this document that may leave the device.
     *
     * Only the recovery-phrase wrap is uploaded. The device wrap is bound to hardware that no other
     * device can reach, and the PIN wrap is what a six-digit secret protects, so uploading either
     * would add attack surface for no recovery benefit. The upload path is expected to call this
     * rather than filter the list itself, so a wrap type added later is excluded by default instead
     * of being uploaded because someone forgot to extend a filter.
     */
    fun uploadableKeyMaterial(): KeyMaterialDocument =
        copy(wrappedKeys = wrapsOf(KeyWrapType.RecoveryPhrase))

    /** Describes the material by which wraps it holds, never by their bytes. */
    override fun toString(): String =
        "KeyMaterialDocument(version=$keyMaterialVersion, wraps=${wrappedKeys.map { it.wrapType }})"

    companion object {
        const val VERSION_FIELD = "key_material_version"
        const val WRAPPED_KEYS_FIELD = "wrapped_keys"

        /** The only key material format this build writes. */
        const val CURRENT_VERSION: Int = 1
    }
}
