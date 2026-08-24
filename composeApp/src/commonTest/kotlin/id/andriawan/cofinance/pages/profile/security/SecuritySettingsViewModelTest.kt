package id.andriawan.cofinance.pages.profile.security

import id.andriawan.cofinance.data.crypto.PhraseExportStatus
import id.andriawan.cofinance.data.crypto.RECOVERY_PHRASE_FILE_NAME
import id.andriawan.cofinance.data.crypto.toRecoveryPhraseExportText
import id.andriawan.cofinance.data.lock.AutoLockTimeout
import id.andriawan.cofinance.data.lock.PinUnlockResult
import id.andriawan.cofinance.pages.encryption.FakeRecoveryPhraseExporter
import id.andriawan.cofinance.pages.lock.LockFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The profile security section, over the real lock.
 *
 * The controls are asserted through their effect rather than through their state flags wherever
 * there is a difference: a PIN change is proved by the new PIN unlocking and the old one failing,
 * not by a success flag, because a screen that reported success without rewrapping the key would
 * pass the second kind of test and lock the user out.
 */
class SecuritySettingsViewModelTest {

    private val fixture = LockFixture()
    private val exporter = FakeRecoveryPhraseExporter()
    private val viewModel =
        SecuritySettingsViewModel(fixture.appLock, fixture.phraseVault, exporter)

    private suspend fun open(pin: String? = LockFixture.PIN): LockFixture.SetUpDevice {
        val device = fixture.completeSetup(pin = pin)
        viewModel.refresh()
        return device
    }

    private fun enterCurrentPin(pin: String) =
        viewModel.onEvent(SecuritySettingsUiEvent.CurrentPinChanged(pin))

    private fun enterNewPin(pin: String) {
        viewModel.onEvent(SecuritySettingsUiEvent.NewPinChanged(pin))
        viewModel.onEvent(SecuritySettingsUiEvent.ConfirmPinChanged(pin))
    }

    // ---- Task 6.6: the controls, the options, and the current-PIN requirement ----

    @Test
    fun theSectionOffersEveryControlOnceEncryptionIsSetUp() = runTest {
        open()

        val state = viewModel.uiState.value
        assertTrue(state.isSetUp)
        assertTrue(state.isPinSet)
        assertFalse(state.isBiometricEnabled)
    }

    @Test
    fun theSectionIsAbsentBeforeEncryptionSetup() = runTest {
        viewModel.refresh()

        assertFalse(
            viewModel.uiState.value.isSetUp,
            "A user with no key material has nothing to lock, so no lock controls are offered"
        )
    }

    @Test
    fun theOfferedTimeoutsAreImmediatelyOneFiveAndFifteenWithOneMinuteInEffect() = runTest {
        open()

        val state = viewModel.uiState.value
        assertEquals(
            listOf(
                AutoLockTimeout.Immediately,
                AutoLockTimeout.OneMinute,
                AutoLockTimeout.FiveMinutes,
                AutoLockTimeout.FifteenMinutes
            ),
            state.autoLockOptions
        )
        assertEquals(
            AutoLockTimeout.OneMinute,
            state.autoLockTimeout,
            "One minute is the value in effect for a user who has never changed it"
        )
        assertEquals(AutoLockTimeout.OneMinute, fixture.autoLockSettings.timeout.value)
    }

    @Test
    fun changingTheTimeoutRequiresTheCurrentPin() = runTest {
        open()

        viewModel.request(SecurityIntent.ChangeAutoLock(AutoLockTimeout.FifteenMinutes))
        val prompt = assertNotNull(viewModel.uiState.value.prompt)
        assertTrue(prompt.requiresCurrentPin)

        enterCurrentPin(LockFixture.WRONG_PIN)
        viewModel.submitPrompt()

        assertIs<SecuritySettingsError.IncorrectPin>(viewModel.uiState.value.prompt?.error)
        assertEquals(
            AutoLockTimeout.OneMinute,
            fixture.autoLockSettings.timeout.value,
            "A rejected PIN must leave the setting alone"
        )

        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()

        assertNull(viewModel.uiState.value.prompt)
        assertEquals(AutoLockTimeout.FifteenMinutes, fixture.autoLockSettings.timeout.value)
        assertEquals(AutoLockTimeout.FifteenMinutes, viewModel.uiState.value.autoLockTimeout)
        assertEquals(SecurityNotice.AutoLockChanged, viewModel.uiState.value.notice)
    }

    @Test
    fun aFirstPinNeedsNoCurrentPinAndMakesTheDeviceUnlockable() = runTest {
        val device = open(pin = null)
        fixture.unlockSessionDirectly(device.dataKey)
        viewModel.refresh()
        assertFalse(viewModel.uiState.value.isPinSet)

        viewModel.request(SecurityIntent.SetPin)
        val prompt = assertNotNull(viewModel.uiState.value.prompt)
        assertFalse(prompt.requiresCurrentPin, "There is no current PIN to require")
        assertTrue(prompt.requiresNewPin)

        enterNewPin(LockFixture.PIN)
        viewModel.submitPrompt()

        assertEquals(SecurityNotice.PinSet, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.isPinSet)

        fixture.appLock.lock()
        assertEquals(PinUnlockResult.Unlocked, fixture.appLock.unlockWithPin(LockFixture.PIN))
    }

    @Test
    fun changingThePinRequiresTheCurrentPinAndRewrapsTheKey() = runTest {
        open()

        viewModel.request(SecurityIntent.ChangePin)
        assertTrue(assertNotNull(viewModel.uiState.value.prompt).requiresCurrentPin)

        enterCurrentPin(LockFixture.WRONG_PIN)
        enterNewPin(LockFixture.OTHER_PIN)
        viewModel.submitPrompt()

        assertIs<SecuritySettingsError.IncorrectPin>(viewModel.uiState.value.prompt?.error)
        assertEquals(
            PinUnlockResult.Unlocked,
            fixture.appLock.unlockWithPin(LockFixture.PIN),
            "A rejected current PIN must leave the old PIN in effect"
        )
        fixture.appLock.lock()

        enterCurrentPin(LockFixture.PIN)
        enterNewPin(LockFixture.OTHER_PIN)
        viewModel.submitPrompt()

        assertEquals(SecurityNotice.PinChanged, viewModel.uiState.value.notice)
        assertEquals(PinUnlockResult.Unlocked, fixture.appLock.unlockWithPin(LockFixture.OTHER_PIN))
    }

    @Test
    fun aNewPinMustBeConfirmedAndMustBeSixDigits() = runTest {
        open()

        viewModel.request(SecurityIntent.ChangePin)
        enterCurrentPin(LockFixture.PIN)
        viewModel.onEvent(SecuritySettingsUiEvent.NewPinChanged(LockFixture.OTHER_PIN))
        viewModel.onEvent(SecuritySettingsUiEvent.ConfirmPinChanged("111111"))
        viewModel.submitPrompt()

        assertEquals(
            SecuritySettingsError.PinConfirmationMismatch,
            viewModel.uiState.value.prompt?.error
        )

        viewModel.onEvent(SecuritySettingsUiEvent.NewPinChanged("123"))
        viewModel.onEvent(SecuritySettingsUiEvent.ConfirmPinChanged("123"))
        viewModel.submitPrompt()

        assertEquals(SecuritySettingsError.PinIncomplete, viewModel.uiState.value.prompt?.error)
        assertEquals(PinUnlockResult.Unlocked, fixture.appLock.unlockWithPin(LockFixture.PIN))
    }

    @Test
    fun removingThePinRequiresTheCurrentPin() = runTest {
        open()

        viewModel.request(SecurityIntent.RemovePin)
        enterCurrentPin(LockFixture.WRONG_PIN)
        viewModel.submitPrompt()

        assertIs<SecuritySettingsError.IncorrectPin>(viewModel.uiState.value.prompt?.error)
        assertTrue(fixture.appLock.isPinSet())

        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()

        assertEquals(SecurityNotice.PinRemoved, viewModel.uiState.value.notice)
        assertFalse(fixture.appLock.isPinSet())
        assertFalse(viewModel.uiState.value.isPinSet)
    }

    @Test
    fun biometricCannotBeTurnedOnWithoutAPinAndSaysWhy() = runTest {
        val device = open(pin = null)
        fixture.unlockSessionDirectly(device.dataKey)
        viewModel.refresh()

        viewModel.request(SecurityIntent.EnableBiometric(fixture.prompt))

        assertEquals(SecuritySettingsError.BiometricRequiresPin, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.prompt, "No PIN prompt is raised for a refusal")
        assertFalse(fixture.appLock.isBiometricEnabled())
    }

    @Test
    fun turningBiometricOnRequiresTheCurrentPin() = runTest {
        open()

        viewModel.request(SecurityIntent.EnableBiometric(fixture.prompt))
        assertTrue(assertNotNull(viewModel.uiState.value.prompt).requiresCurrentPin)

        enterCurrentPin(LockFixture.WRONG_PIN)
        viewModel.submitPrompt()

        assertNotNull(viewModel.uiState.value.prompt?.error)
        assertFalse(fixture.appLock.isBiometricEnabled())

        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()

        assertEquals(SecurityNotice.BiometricEnabled, viewModel.uiState.value.notice)
        assertTrue(fixture.appLock.isBiometricEnabled())
        assertTrue(viewModel.uiState.value.isBiometricEnabled)
    }

    @Test
    fun turningBiometricOffLeavesThePinWorking() = runTest {
        open()
        fixture.appLock.enableBiometric(LockFixture.PIN, fixture.prompt)
        viewModel.refresh()
        assertTrue(viewModel.uiState.value.isBiometricEnabled)

        viewModel.request(SecurityIntent.DisableBiometric)
        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()

        assertEquals(SecurityNotice.BiometricDisabled, viewModel.uiState.value.notice)
        assertFalse(fixture.appLock.isBiometricEnabled())
        assertEquals(PinUnlockResult.Unlocked, fixture.appLock.unlockWithPin(LockFixture.PIN))
    }

    // ---- Task 6.8: recovery-phrase re-display behind fresh PIN entry ----

    @Test
    fun thePhraseIsShownOnlyAfterTheCorrectPin() = runTest {
        val device = open()

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)
        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty(), "Nothing shows before the PIN")

        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()

        assertEquals(device.phrase.words, viewModel.uiState.value.revealedPhrase)
        assertNull(viewModel.uiState.value.prompt)
    }

    @Test
    fun theRevealedPhraseCanBeCopiedOrSavedAndOnlyWhileItIsOnScreen() = runTest {
        val device = open()

        // Nothing is on screen yet, so there is nothing to hand to the clipboard.
        viewModel.copyRevealedPhrase()
        assertNull(exporter.copiedText)
        assertNull(viewModel.uiState.value.exportStatus)

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)
        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()

        val expected = device.phrase.words.toRecoveryPhraseExportText()

        viewModel.copyRevealedPhrase()
        assertEquals(expected, exporter.copiedText)
        assertEquals(PhraseExportStatus.Copied, viewModel.uiState.value.exportStatus)

        viewModel.downloadRevealedPhrase()
        assertEquals(expected, exporter.savedText)
        assertEquals(RECOVERY_PHRASE_FILE_NAME, exporter.savedFileName)

        viewModel.onEvent(SecuritySettingsUiEvent.RecoveryPhraseDismissed)
        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty())
        assertNull(viewModel.uiState.value.exportStatus)
    }

    @Test
    fun aWrongPinShowsNothing() = runTest {
        open()

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)
        enterCurrentPin(LockFixture.WRONG_PIN)
        viewModel.submitPrompt()

        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty())
        assertIs<SecuritySettingsError.IncorrectPin>(viewModel.uiState.value.prompt?.error)
    }

    @Test
    fun dismissingThePromptShowsNothing() = runTest {
        open()

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)
        viewModel.onEvent(SecuritySettingsUiEvent.PromptDismissed)

        assertNull(viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty())
    }

    @Test
    fun anAlreadyUnlockedSessionIsStillAskedForThePin() = runTest {
        val device = open()
        fixture.unlockSessionDirectly(device.dataKey)
        viewModel.refresh()

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)

        assertNotNull(
            viewModel.uiState.value.prompt,
            "Holding the data key in memory is not a substitute for entering the PIN"
        )
        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty())

        // And a wrong PIN is still refused, so the held key is genuinely not being used.
        enterCurrentPin(LockFixture.WRONG_PIN)
        viewModel.submitPrompt()
        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty())
    }

    @Test
    fun everyRequestIsPromptedAgain() = runTest {
        val device = open()

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)
        enterCurrentPin(LockFixture.PIN)
        viewModel.submitPrompt()
        assertEquals(device.phrase.words, viewModel.uiState.value.revealedPhrase)

        viewModel.onEvent(SecuritySettingsUiEvent.RecoveryPhraseDismissed)
        viewModel.request(SecurityIntent.RevealRecoveryPhrase)

        assertNotNull(viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.revealedPhrase.isEmpty())
    }

    @Test
    fun aFailedPinInSettingsCountsAgainstTheSameGuardAsTheUnlockScreen() = runTest {
        open()

        viewModel.request(SecurityIntent.RevealRecoveryPhrase)
        enterCurrentPin(LockFixture.WRONG_PIN)
        viewModel.submitPrompt()

        val error = assertIs<SecuritySettingsError.IncorrectPin>(viewModel.uiState.value.prompt?.error)
        assertEquals(
            9,
            error.attemptsRemaining,
            "Settings must not be an unthrottled PIN oracle beside the unlock screen"
        )
    }
}
