package id.andriawan.cofinance.data.lock

import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A bare `FragmentActivity`, declared in the device test manifest, to stand in for the app's own. */
class BiometricHostTestActivity : FragmentActivity()

/**
 * Device test for the seam between the app's activity and `androidx.biometric`.
 *
 * A successful biometric unlock cannot be automated — no test API presents a finger — so what is
 * asserted here is the thing that was actually broken: with no host, every capability query returns
 * `Unavailable` regardless of the hardware, and with a real `FragmentActivity` installed the same
 * query reaches `BiometricManager` and answers about the device. That is the whole content of task
 * 7a.4, and it is why the app's `MainActivity` had to stop being a bare `ComponentActivity`.
 *
 * The activity used here is a plain `FragmentActivity` rather than the app's, because the app's
 * lives in the `androidApp` module and is not on this module's classpath. The property under test
 * is a property of the type, not of that particular subclass.
 */
class AndroidBiometricPromptHostTest {

    @After
    fun tearDown() {
        BiometricPromptHost.current = null
    }

    @Test
    fun withNoHostEveryPromptReportsUnavailable() = runBlocking {
        BiometricPromptHost.current = null

        // The pre-wiring state, asserted so the test below is a change rather than a coincidence.
        assertEquals(BiometricCapability.Unavailable, createBiometricKeyBox().capability())
    }

    @Test
    fun aFragmentActivityHostLetsTheCapabilityQueryReachTheHardware() {
        ActivityScenario.launch(BiometricHostTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> BiometricPromptHost.current = activity }

            val capability = runBlocking { createBiometricKeyBox().capability() }

            // Not asserting Available: that depends on what is enrolled on the device running this.
            // What must hold is that the answer now comes from BiometricManager rather than from
            // the missing-host branch.
            assertNotEquals(BiometricCapability.Unavailable, capability)
            assertTrue(
                capability in setOf(
                    BiometricCapability.Available,
                    BiometricCapability.NotEnrolled,
                    BiometricCapability.NoHardware
                )
            )
        }
    }

    @Test
    fun theHostAcceptsOnlyAFragmentActivity() {
        ActivityScenario.launch(BiometricHostTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                BiometricPromptHost.current = activity
                // Reads as a tautology and is not: the field's type is what forces the app's host
                // activity to be a FragmentActivity, and it is the constraint 7a.4 exists to meet.
                val host: FragmentActivity? = BiometricPromptHost.current
                assertTrue(host is FragmentActivity)
            }
        }
    }
}
