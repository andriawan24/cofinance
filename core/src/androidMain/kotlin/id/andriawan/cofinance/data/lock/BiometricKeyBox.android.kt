package id.andriawan.cofinance.data.lock

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

/**
 * Supplies the activity a biometric prompt has to be shown from.
 *
 * `BiometricPrompt` is hosted by a `FragmentActivity`, and this module has no activity of its own,
 * so the host application hands one over here — the same shape as the content provider that gives
 * the device key vault its context, and for the same reason.
 *
 * **For whoever builds the unlock screen and the profile security section:** the app's
 * `MainActivity` currently extends `ComponentActivity`, which is not a `FragmentActivity`. It has
 * to become one — `androidx.fragment.app.FragmentActivity` or `AppCompatActivity`, both of which
 * `ComponentActivity` is a supertype of — and set [current] in `onCreate` and clear it in
 * `onDestroy`. Until that happens every prompt reports [BiometricCapability.Unavailable] rather
 * than crashing, and the PIN path is unaffected.
 */
object BiometricPromptHost {

    /** The activity to show prompts from, or null while none is resumed. */
    @Volatile
    var current: FragmentActivity? = null
}

/**
 * The Android [BiometricKeyBox]: AES-256-GCM under a Keystore key gated on a strong biometric.
 *
 * Two properties of the key are the whole point of this class, and both are set at generation
 * because neither can be added later:
 *
 * - `setUserAuthenticationRequired(true)` with a per-use validity, so the key is usable only inside
 *   a `CryptoObject` that a successful authentication has released. Holding the sealed file is not
 *   enough, and neither is being the app.
 * - `setInvalidatedByBiometricEnrollment(true)`, so enrolling a new fingerprint or face permanently
 *   destroys the key. That is the correct behavior rather than an inconvenience: a key that
 *   survived enrollment changes would grant the app's data to a finger the user added afterwards,
 *   possibly not their own. The cost is bounded by Decision 4 — the PIN is mandatory, so the user
 *   re-enters six digits and re-enables biometric, rather than restoring from 12 words.
 *
 * When the platform reports the key as invalidated, the sealed copy is deleted here rather than
 * left behind, so the state after an enrollment change is exactly the state before biometric was
 * ever enabled.
 */
class AndroidBiometricKeyBox internal constructor(
    private val directory: () -> File,
    private val activity: () -> FragmentActivity?
) : BiometricKeyBox {

    override suspend fun capability(): BiometricCapability = withContext(Dispatchers.Main) {
        val host = activity() ?: return@withContext BiometricCapability.Unavailable
        when (BiometricManager.from(host).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricCapability.NoHardware

            else -> BiometricCapability.Unavailable
        }
    }

    override suspend fun hasSealedSecret(): Boolean =
        withContext(Dispatchers.IO) { sealedFile().isFile && keyExists() }

    override suspend fun seal(
        plaintext: ByteArray,
        prompt: BiometricPromptText
    ): BiometricSealResult {
        val cipher = try {
            // A fresh key per enable, so re-enabling after an invalidation cannot reuse a key the
            // platform has already decided is untrustworthy.
            deleteKeyAndSecret()
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, generateKey())
            }
        } catch (cause: GeneralSecurityException) {
            return BiometricSealResult.Failed(cause.message.orEmpty())
        }

        return when (val authentication = authenticate(prompt, cipher)) {
            is Authentication.Succeeded -> try {
                val sealed = frame(
                    authentication.cipher.iv,
                    authentication.cipher.doFinal(plaintext)
                )
                withContext(Dispatchers.IO) { sealedFile().writeBytes(sealed) }
                BiometricSealResult.Sealed
            } catch (cause: GeneralSecurityException) {
                BiometricSealResult.Failed(cause.message.orEmpty())
            }

            Authentication.Cancelled -> BiometricSealResult.Cancelled
            Authentication.Unavailable -> BiometricSealResult.Unavailable
            is Authentication.Failed -> BiometricSealResult.Failed(authentication.reason)
        }
    }

    override suspend fun open(prompt: BiometricPromptText): BiometricOpenResult {
        val sealed = withContext(Dispatchers.IO) {
            sealedFile().takeIf { it.isFile }?.readBytes()
        } ?: return BiometricOpenResult.Absent
        val key = existingKey() ?: run {
            // The file outlived its key, which is what a cleared Keystore or a restored backup
            // looks like. Nothing can open it, so it goes.
            clear()
            return BiometricOpenResult.Invalidated
        }

        val ivLength = sealed.firstOrNull()?.toInt()?.and(0xFF) ?: return BiometricOpenResult.Absent
        if (sealed.size <= 1 + ivLength) return BiometricOpenResult.Absent

        val cipher = try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(GCM_TAG_BITS, sealed, 1, ivLength)
                )
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            // The enrolled biometrics changed. This is the policy working; the PIN still opens the
            // data key through an entirely separate derivation.
            clear()
            return BiometricOpenResult.Invalidated
        } catch (cause: GeneralSecurityException) {
            return BiometricOpenResult.Failed(cause.message.orEmpty())
        }

        return when (val authentication = authenticate(prompt, cipher)) {
            is Authentication.Succeeded -> try {
                BiometricOpenResult.Opened(
                    authentication.cipher.doFinal(
                        sealed,
                        1 + ivLength,
                        sealed.size - 1 - ivLength
                    )
                )
            } catch (cause: GeneralSecurityException) {
                BiometricOpenResult.Failed(cause.message.orEmpty())
            }

            Authentication.Cancelled -> BiometricOpenResult.Cancelled
            Authentication.Unavailable -> BiometricOpenResult.Unavailable
            is Authentication.Failed -> BiometricOpenResult.Failed(authentication.reason)
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { deleteKeyAndSecret() }
    }

    // -------------------------------------------------------------------------------------------

    private fun deleteKeyAndSecret() {
        sealedFile().delete()
        val keyStore = keyStore()
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun sealedFile(): File = File(directory(), SEALED_KEY_FILE)

    private fun frame(iv: ByteArray, ciphertext: ByteArray): ByteArray =
        ByteArray(1 + iv.size + ciphertext.size).also { out ->
            out[0] = iv.size.toByte()
            iv.copyInto(out, 1)
            ciphertext.copyInto(out, 1 + iv.size)
        }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private fun keyExists(): Boolean = keyStore().containsAlias(KEY_ALIAS)

    private fun existingKey(): SecretKey? {
        val keyStore = keyStore()
        if (!keyStore.containsAlias(KEY_ALIAS)) return null
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    /**
     * Generates the biometric-gated key.
     *
     * The authentication parameters differ by API level only in how they are expressed: from API 30
     * the accepted authenticator types are named explicitly, and below it a validity duration of -1
     * is the documented way to say "authenticate for every use".
     */
    @Suppress("DEPRECATION")
    internal fun generateKey(): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(AES_KEY_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private sealed interface Authentication {
        data class Succeeded(val cipher: Cipher) : Authentication
        data object Cancelled : Authentication
        data object Unavailable : Authentication
        data class Failed(val reason: String) : Authentication
    }

    private suspend fun authenticate(
        text: BiometricPromptText,
        cipher: Cipher
    ): Authentication = withContext(Dispatchers.Main) {
        val host = activity() ?: return@withContext Authentication.Unavailable

        suspendCancellableCoroutine<Authentication> { continuation ->
            val prompt = BiometricPrompt(
                host,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        val used = result.cryptoObject?.cipher
                        if (continuation.isActive) {
                            continuation.resume(
                                if (used == null) {
                                    Authentication.Failed("no cipher was returned")
                                } else {
                                    Authentication.Succeeded(used)
                                }
                            )
                        }
                    }

                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        if (!continuation.isActive) return
                        continuation.resume(
                            when (code) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                BiometricPrompt.ERROR_CANCELED -> Authentication.Cancelled

                                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                                BiometricPrompt.ERROR_NO_BIOMETRICS,
                                BiometricPrompt.ERROR_LOCKOUT,
                                BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                    Authentication.Unavailable

                                else -> Authentication.Failed("$code: $message")
                            }
                        )
                    }

                    // Deliberately not resumed: a single non-matching finger is not the end of the
                    // attempt, and the prompt stays up until the user succeeds or leaves.
                    override fun onAuthenticationFailed() = Unit
                }
            )

            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(text.title)
                .apply { text.subtitle?.let(::setSubtitle) }
                .setNegativeButtonText(text.negativeButtonLabel)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()

            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        }
    }

    companion object {
        /** Keystore alias of the biometric-gated key. Exposed for the device test. */
        const val KEY_ALIAS: String = "id.andriawan.cofinance.lock.biometric"

        /** File holding the biometrically sealed copy of the data key. */
        const val SEALED_KEY_FILE: String = "biometric-data-key.bin"

        /**
         * Class 3 biometrics only.
         *
         * A weaker class cannot gate a Keystore key at all, and device credential is deliberately
         * excluded: the app lock exists to protect against someone holding an unlocked phone, and
         * the device credential is exactly what that person already has.
         */
        private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
    }
}

actual fun createBiometricKeyBox(): BiometricKeyBox = AndroidBiometricKeyBox(
    directory = { LockStorage.directory() },
    activity = { BiometricPromptHost.current }
)
