package id.andriawan.cofinance.data.lock

/** Whether biometric authentication can be offered on this device right now. */
enum class BiometricCapability {

    /** Hardware exists, a biometric is enrolled, and it may be used. */
    Available,

    /** Hardware exists but nothing is enrolled, so the user would have to enroll first. */
    NotEnrolled,

    /** No biometric hardware, or none strong enough to protect a key. */
    NoHardware,

    /** Temporarily unusable — hardware busy, or locked out after too many biometric failures. */
    Unavailable
}

/**
 * The words the platform prompt shows.
 *
 * Supplied by the caller rather than built here, because the strings are localized resources that
 * belong to the presentation layer. [negativeButtonLabel] is the fallback affordance and should
 * always name the PIN: per Decision 4 the PIN is the floor, and every route out of the prompt leads
 * back to it.
 */
data class BiometricPromptText(
    val title: String,
    val subtitle: String? = null,
    val negativeButtonLabel: String
)

/** The outcome of sealing bytes behind biometric authentication. */
sealed interface BiometricSealResult {
    data object Sealed : BiometricSealResult
    data object Cancelled : BiometricSealResult
    data class Failed(val reason: String) : BiometricSealResult
    data object Unavailable : BiometricSealResult
}

/** The outcome of opening the sealed secret. */
sealed interface BiometricOpenResult {

    /** Authentication succeeded and the secret is returned. */
    data class Opened(val plaintext: ByteArray) : BiometricOpenResult {

        override fun equals(other: Any?): Boolean =
            this === other || (other is Opened && plaintext.contentEquals(other.plaintext))

        override fun hashCode(): Int = plaintext.contentHashCode()

        override fun toString(): String = "Opened(${plaintext.size} bytes)"
    }

    /** The user dismissed the prompt, which is the caller's cue to offer PIN entry. */
    data object Cancelled : BiometricOpenResult

    /** Authentication was attempted and did not succeed. */
    data class Failed(val reason: String) : BiometricOpenResult

    /**
     * The key was destroyed by a change to the device's enrolled biometrics.
     *
     * This is the enrollment-invalidation policy working as intended, and the implementation has
     * already discarded what is left of the sealed secret. The caller falls back to the PIN, which
     * per Decision 4 is why the PIN is mandatory: this event costs six digits rather than a
     * 12-word restore.
     */
    data object Invalidated : BiometricOpenResult

    /** Nothing is sealed, because biometric unlock was never enabled or has been turned off. */
    data object Absent : BiometricOpenResult

    /** Biometric authentication cannot be offered right now. */
    data object Unavailable : BiometricOpenResult
}

/**
 * A small amount of bytes held behind a biometric-gated, enrollment-invalidating platform key.
 *
 * This is the whole platform surface biometric unlock needs. What is sealed — a data key and the
 * identifier that names it — is decided in common code by [BiometricUnlock], so neither platform
 * implementation touches the key hierarchy, and the common part is testable against a fake box.
 *
 * Two properties are required of every implementation and are what the platform tests assert:
 *
 * - **The key requires authentication per use.** Possession of the sealed bytes is not enough; a
 *   successful biometric authentication must be part of opening them.
 * - **A change to the enrolled biometric set invalidates the key.** Adding a fingerprint destroys
 *   it rather than extending access to the new finger. [BiometricOpenResult.Invalidated] is how
 *   that surfaces, and the PIN path is unaffected because it derives from an entirely different
 *   key — see `PinKeyWrapper`.
 */
interface BiometricKeyBox {
    suspend fun capability(): BiometricCapability
    suspend fun hasSealedSecret(): Boolean
    suspend fun seal(plaintext: ByteArray, prompt: BiometricPromptText): BiometricSealResult
    suspend fun open(prompt: BiometricPromptText): BiometricOpenResult
    suspend fun clear()
}

/** Returns the biometric-gated box backed by this platform. */
expect fun createBiometricKeyBox(): BiometricKeyBox
