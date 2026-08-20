package id.andriawan.cofinance.pages.lock

import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

/**
 * What launch does about the lock, which is the whole of "the lock does not gate local-only use".
 *
 * The interesting case is the first one. A user who has never signed in has no key material, no
 * PIN, and nothing synchronized; the phrase and the lock both exist to protect the copy that leaves
 * the device, and that user has no such copy. They must reach the app directly, and this asserts it
 * against a real lock over a device that has had nothing set up on it, rather than against a
 * hand-made state value.
 */
class LaunchLockDecisionTest {

    @Test
    fun aLocalOnlyLaunchGoesStraightToTheMainExperience() = runTest {
        val fixture = LockFixture()

        // Nothing has been set up: no sign-in, no encryption setup, no PIN.
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
        assertFalse(fixture.appLock.isPinSet())
        assertFalse(fixture.appLock.isBiometricEnabled())

        assertEquals(
            LaunchLockDecision.Main,
            launchLockDecision(fixture.appLock.state.value),
            "A user who has never completed encryption setup must not be shown an unlock screen"
        )
    }

    @Test
    fun aDeviceThatCompletedSetupUnlocksBeforeFinanceDataIsShown() = runTest {
        val fixture = LockFixture()
        fixture.completeSetup()

        assertEquals(EncryptionSessionState.Locked, fixture.session.state.value)
        assertEquals(LaunchLockDecision.Unlock, launchLockDecision(fixture.appLock.state.value))
    }

    @Test
    fun anAlreadyUnlockedSessionIsNotAskedAgain() = runTest {
        val fixture = LockFixture()
        val device = fixture.completeSetup()
        fixture.unlockSessionDirectly(device.dataKey)

        assertEquals(LaunchLockDecision.Main, launchLockDecision(fixture.appLock.state.value))
    }

    @Test
    fun destroyedKeyMaterialLeavesLaunchOutOfTheLockRatherThanStuckOnIt() = runTest {
        val fixture = LockFixture()
        fixture.completeSetup()

        repeat(10) {
            fixture.appLock.unlockWithPin(LockFixture.WRONG_PIN)
            fixture.clock.advanceBy(10 * 60 * 1000)
        }

        // The session is back to SetupIncomplete, so launch routes past the lock — to setup or
        // restore, which is where the recovery phrase is asked for.
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
        assertEquals(LaunchLockDecision.Main, launchLockDecision(fixture.appLock.state.value))
    }
}
