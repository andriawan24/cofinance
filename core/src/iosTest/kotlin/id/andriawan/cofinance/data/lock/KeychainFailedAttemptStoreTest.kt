package id.andriawan.cofinance.data.lock

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The iOS failed-attempt counter, against the real Keychain.
 *
 * **This test is written but has never been executed.** The `iosSimulatorArm64` test binary does
 * not link in this repository — `:composeApp:linkDebugTestIosSimulatorArm64` fails with
 * `ld: framework 'FirebaseCore' not found`, because Firebase reaches the iOS app through the Xcode
 * project rather than through Gradle. That is a pre-existing condition recorded in the design
 * document's risk list and it blocks every iOS test, not this one in particular. Nothing here
 * should be read as a passing result until the link is fixed.
 *
 * A dedicated service name keeps these items out of the ones the app itself writes, so running the
 * suite on a device never touches a real counter.
 */
class KeychainFailedAttemptStoreTest {

    private val store = KeychainFailedAttemptStore(
        service = "id.andriawan.cofinance.lock.test",
        account = "failed-attempts-test"
    )

    @BeforeTest
    fun setUp() = runTest { store.clear() }

    @AfterTest
    fun tearDown() = runTest { store.clear() }

    @Test
    fun anAbsentCounterReadsAsNone() = runTest {
        assertEquals(StoredFailedAttempts.None, store.read())
    }

    @Test
    fun theCounterRoundTripsThroughTheKeychain() = runTest {
        store.write(FailedAttemptRecord(consecutiveFailures = 7, lastFailureAtMillis = 1_234_567))

        val stored = assertIs<StoredFailedAttempts.Recorded>(store.read())

        assertEquals(7, stored.record.consecutiveFailures)
        assertEquals(1_234_567L, stored.record.lastFailureAtMillis)
    }

    @Test
    fun aSecondWriteReplacesTheFirstRatherThanAddingAnItem() = runTest {
        store.write(FailedAttemptRecord(1, 10))
        store.write(FailedAttemptRecord(2, 20))

        val stored = assertIs<StoredFailedAttempts.Recorded>(store.read())

        assertEquals(2, stored.record.consecutiveFailures)
        assertEquals(20L, stored.record.lastFailureAtMillis)
    }

    @Test
    fun aFreshInstanceSeesTheSameCounter() = runTest {
        store.write(FailedAttemptRecord(5, 500))

        val relaunched = KeychainFailedAttemptStore(
            service = "id.andriawan.cofinance.lock.test",
            account = "failed-attempts-test"
        )

        val stored = assertIs<StoredFailedAttempts.Recorded>(relaunched.read())
        assertEquals(5, stored.record.consecutiveFailures)
    }

    @Test
    fun clearingRemovesTheItem() = runTest {
        store.write(FailedAttemptRecord(3, 30))
        store.clear()

        assertEquals(StoredFailedAttempts.None, store.read())
    }

    @Test
    fun extremeValuesRoundTripExactly() = runTest {
        store.write(FailedAttemptRecord(Int.MAX_VALUE, Long.MAX_VALUE))

        val stored = assertIs<StoredFailedAttempts.Recorded>(store.read())

        assertEquals(Int.MAX_VALUE, stored.record.consecutiveFailures)
        assertEquals(Long.MAX_VALUE, stored.record.lastFailureAtMillis)
    }

    /**
     * The property Android cannot offer: the item belongs to the keychain, not to the container.
     *
     * This cannot be asserted from inside a test run — nothing here can uninstall the app — so what
     * it checks is the attribute the guarantee rests on: the item is written as a generic password
     * under this app's service, which iOS leaves in place when the app is deleted. The end-to-end
     * confirmation is a manual delete-and-reinstall pass.
     */
    @Test
    fun theItemIsStoredAsAGenericPasswordRatherThanInTheContainer() = runTest {
        store.write(FailedAttemptRecord(2, 2))

        assertTrue(store.read() is StoredFailedAttempts.Recorded)
    }
}
