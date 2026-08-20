package id.andriawan.cofinance.pages.lock

import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.lock.BiometricOpenResult
import id.andriawan.cofinance.data.lock.PinFallbackReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest

/**
 * The unlock screen, driven over the real lock.
 *
 * Two of these are about copy rather than cryptography, and are here because getting them wrong is
 * what makes a working lock look broken: a throttled attempt has to say how long, and the tenth
 * failure has to point at the recovery phrase rather than at nothing.
 */
class UnlockViewModelTest {

    private val fixture = LockFixture()
    private val viewModel = UnlockViewModel(fixture.appLock)

    @Test
    fun theCorrectPinUnlocksTheSession() = runTest {
        fixture.completeSetup()

        viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.PIN))
        viewModel.submit()

        assertTrue(viewModel.uiState.value.isUnlocked)
        assertEquals(EncryptionSessionState.Unlocked, fixture.session.state.value)
        // The entered PIN is not left sitting in state after it has been spent.
        assertEquals("", viewModel.uiState.value.pin)
    }

    @Test
    fun aWrongPinReportsHowManyAttemptsAreLeftAndUnlocksNothing() = runTest {
        fixture.completeSetup()

        viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.WRONG_PIN))
        viewModel.submit()

        val feedback = assertIs<UnlockFeedback.IncorrectPin>(viewModel.uiState.value.feedback)
        assertEquals(9, feedback.attemptsRemaining)
        assertEquals(Duration.ZERO, feedback.nextAttemptDelay)
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertEquals(EncryptionSessionState.Locked, fixture.session.state.value)
    }

    @Test
    fun theFifthFailureReportsTheWaitBeforeTheNextAttempt() = runTest {
        fixture.completeSetup()

        repeat(5) {
            viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.WRONG_PIN))
            viewModel.submit()
            // Past the delay each time, so that this test measures the schedule rather than the
            // throttle it produces; the throttle has its own test below.
            fixture.clock.advanceBy(10 * 60 * 1000)
        }

        val feedback = assertIs<UnlockFeedback.IncorrectPin>(viewModel.uiState.value.feedback)
        assertEquals(5, feedback.attemptsRemaining)
        assertTrue(
            feedback.nextAttemptDelay > Duration.ZERO,
            "The sixth attempt has to be reported as delayed, or the wait looks like a hang"
        )
    }

    @Test
    fun anAttemptInsideTheDelayIsReportedAsAWaitRatherThanAsAWrongPin() = runTest {
        fixture.completeSetup()

        // Four failures, each after the previous delay has elapsed, then a fifth that leaves a live
        // delay behind it.
        repeat(4) {
            viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.WRONG_PIN))
            viewModel.submit()
            fixture.clock.advanceBy(10 * 60 * 1000)
        }
        viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.WRONG_PIN))
        viewModel.submit()

        // No time passes before this one, so the escalating delay has not elapsed. The PIN is the
        // correct one, which is the point: a throttled attempt is refused before any derivation.
        viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.PIN))
        viewModel.submit()

        val feedback = assertIs<UnlockFeedback.Throttled>(viewModel.uiState.value.feedback)
        assertTrue(feedback.remaining > Duration.ZERO)
        assertFalse(viewModel.uiState.value.isUnlocked)
    }

    @Test
    fun theTenthFailureRoutesToRecoveryPhraseRestoreRatherThanToAnError() = runTest {
        fixture.completeSetup()

        repeat(10) {
            viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.WRONG_PIN))
            viewModel.submit()
            fixture.clock.advanceBy(10 * 60 * 1000)
        }

        assertTrue(
            viewModel.uiState.value.requiresRecoveryPhrase,
            "Destroyed key material is recoverable, so the screen must send the user to restore"
        )
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
    }

    @Test
    fun aDeviceWithNoPinSaysSoBeforeAnythingIsTyped() = runTest {
        fixture.completeSetup(pin = null)

        viewModel.prepare()

        assertEquals(UnlockFeedback.PinNotSet, viewModel.uiState.value.feedback)
    }

    @Test
    fun biometricUnlockSkipsThePinEntirely() = runTest {
        val device = fixture.completeSetup()
        fixture.appLock.enableBiometric(LockFixture.PIN, fixture.prompt)

        viewModel.prepare()
        assertTrue(viewModel.uiState.value.isBiometricOffered)

        viewModel.unlockWithBiometric(fixture.prompt)

        assertTrue(viewModel.uiState.value.isUnlocked)
        assertEquals(device.dataKey.id, fixture.session.dataKeyOrNull()?.id)
    }

    @Test
    fun aDismissedBiometricPromptFallsBackToPinWithoutSpendingAnAttempt() = runTest {
        fixture.completeSetup()
        fixture.appLock.enableBiometric(LockFixture.PIN, fixture.prompt)
        fixture.biometricBox.openResult = BiometricOpenResult.Cancelled

        viewModel.unlockWithBiometric(fixture.prompt)

        val feedback = assertIs<UnlockFeedback.BiometricFellBack>(viewModel.uiState.value.feedback)
        assertEquals(PinFallbackReason.Cancelled, feedback.reason)
        assertFalse(viewModel.uiState.value.isUnlocked)

        // And the PIN still opens the session on the first try, which it would not if a dismissed
        // prompt had been counted as a failed PIN attempt.
        viewModel.onEvent(UnlockUiEvent.PinChanged(LockFixture.PIN))
        viewModel.submit()
        assertTrue(viewModel.uiState.value.isUnlocked)
    }

    @Test
    fun anIncompletePinCannotBeSubmitted() = runTest {
        fixture.completeSetup()

        viewModel.onEvent(UnlockUiEvent.PinChanged("12a"))

        assertEquals("12", viewModel.uiState.value.pin)
        assertFalse(viewModel.uiState.value.canSubmit)
    }
}
