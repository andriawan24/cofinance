package id.andriawan.cofinance.data.lock

import android.os.Build
import android.security.keystore.KeyInfo
import androidx.biometric.BiometricManager
import androidx.test.core.app.ApplicationProvider
import id.andriawan.cofinance.data.crypto.DeviceKeyVaultStorage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory

/**
 * Device test for the biometric-gated key.
 *
 * ## What can be automated here, and what cannot
 *
 * A biometric prompt needs a finger. No instrumented test can supply one on a physical device —
 * there is no test API that authenticates on the user's behalf — so the *successful biometric
 * unlock* path is verified by the common tests against a fake box plus a manual pass, and what is
 * asserted here is everything about the key that does not require a prompt:
 *
 * - the key is created with authentication required and with enrollment invalidation on, which is
 *   the policy Decision 4 asks for,
 * - its material never leaves the Keystore,
 * - it cannot be used without authentication, so possession of the sealed file is not enough,
 * - and losing the key — which is exactly what an enrollment change does to it — leaves the sealed
 *   copy discarded and the PIN path untouched.
 *
 * The last one is how a "simulated enrollment change" is expressed on Android: an enrollment change
 * makes the key permanently unusable and the Keystore reports it through
 * `KeyPermanentlyInvalidatedException`. Deleting the entry produces the same observable state for
 * the app — a sealed blob whose key is gone — and the assertion is that the app cleans up and does
 * not lose the PIN.
 *
 * Tests that need a key with `setUserAuthenticationRequired(true)` are skipped on a device with no
 * enrolled biometric, because Android refuses to generate such a key at all there. The skip is
 * reported rather than silent.
 */
class AndroidBiometricKeyBoxTest {

    private lateinit var box: AndroidBiometricKeyBox

    private val biometricsEnrolled: Boolean
        get() = BiometricManager
            .from(ApplicationProvider.getApplicationContext())
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    @Before
    fun setUp() = runBlocking {
        box = createBiometricKeyBox() as AndroidBiometricKeyBox
        box.clear()
    }

    @After
    fun tearDown() = runBlocking {
        box.clear()
    }

    @Test
    fun theKeyRequiresAuthenticationAndIsInvalidatedByEnrollmentChanges() {
        if (!biometricsEnrolled) {
            println("skipped: no strong biometric is enrolled on this device")
            return
        }

        val key = box.generateKey()
        val info = SecretKeyFactory
            .getInstance(key.algorithm, "AndroidKeyStore")
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo

        assertTrue("the key does not require authentication", info.isUserAuthenticationRequired)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // KeyInfo only started reporting this flag in API 31; below that the request is made
            // at generation and cannot be read back, so there is nothing to assert.
            assertTrue(
                "the key survives biometric enrollment changes",
                info.isInvalidatedByBiometricEnrollment
            )
        }
    }

    @Test
    fun theKeyMaterialNeverLeavesTheKeystore() {
        if (!biometricsEnrolled) {
            println("skipped: no strong biometric is enrolled on this device")
            return
        }

        box.generateKey()

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey(AndroidBiometricKeyBox.KEY_ALIAS, null)

        assertNotNull(key)
        assertNull("the biometric key material escaped the keystore", key.encoded)
    }

    @Test
    fun theKeyCannotBeUsedWithoutAuthentication() {
        if (!biometricsEnrolled) {
            println("skipped: no strong biometric is enrolled on this device")
            return
        }

        val key = box.generateKey()

        // Possession of the key handle is not authorization: some releases refuse at init and some
        // at the operation, so both are accepted, and only a successful encryption is a failure.
        val produced = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.doFinal(ByteArray(32))
        } catch (_: Exception) {
            null
        }

        assertNull("the biometric key encrypted without any authentication", produced)
    }

    @Test
    fun aSealedCopyWhoseKeyIsGoneIsDiscardedAndReportedAsInvalidated() = runBlocking {
        // The state an enrollment change leaves behind: a sealed blob and no usable key. Written
        // directly, because sealing it for real would need a prompt.
        val file = File(lockDirectory(), AndroidBiometricKeyBox.SEALED_KEY_FILE)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(64) { it.toByte() })
        deleteKey()

        val result = box.open(BiometricPromptText("Unlock", negativeButtonLabel = "Use PIN"))

        assertEquals(BiometricOpenResult.Invalidated, result)
        assertFalse("the unusable sealed copy was left on disk", file.exists())
        assertFalse(box.hasSealedSecret())
    }

    @Test
    fun anAbsentSealedCopyIsReportedAsAbsentRatherThanAsAFailure() = runBlocking {
        assertEquals(
            BiometricOpenResult.Absent,
            box.open(BiometricPromptText("Unlock", negativeButtonLabel = "Use PIN"))
        )
        assertFalse(box.hasSealedSecret())
    }

    @Test
    fun capabilityIsReportedWithoutAnActivityRatherThanCrashing() = runBlocking {
        // No activity is hosted by an instrumented test of a library, which is the same state the
        // app is in before the unlock screen is on screen. It must be an answer, not an exception.
        assertEquals(BiometricCapability.Unavailable, box.capability())
    }

    private fun lockDirectory(): File =
        File(DeviceKeyVaultStorage.applicationContext!!.filesDir, "app-lock")

    private fun deleteKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AndroidBiometricKeyBox.KEY_ALIAS)) {
            keyStore.deleteEntry(AndroidBiometricKeyBox.KEY_ALIAS)
        }
    }
}
