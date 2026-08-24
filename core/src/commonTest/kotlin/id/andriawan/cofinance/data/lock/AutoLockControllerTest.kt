package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Auto-lock, with time supplied rather than waited for.
 *
 * Both of the controller's paths are covered: the timer that fires while the app is in the
 * background, and the elapsed-time check on return that covers a process which was frozen and never
 * ran the timer. The second is driven by advancing the [LockClock] *without* advancing the
 * scheduler, which is exactly what a suspended process looks like from inside the app.
 */
class AutoLockControllerTest {

    @Test
    fun oneMinuteIsInEffectForAUserWhoNeverChoseAndTheOptionsAreTheFour() {
        val settings = KeyValueAutoLockSettings(readStoredId = { null }, writeStoredId = {})

        assertEquals(AutoLockTimeout.OneMinute, settings.timeout.value)
        assertEquals(
            listOf(
                AutoLockTimeout.Immediately,
                AutoLockTimeout.OneMinute,
                AutoLockTimeout.FiveMinutes,
                AutoLockTimeout.FifteenMinutes
            ),
            AutoLockTimeout.options
        )
    }

    @Test
    fun theChosenTimeoutIsStoredAndReadBack() {
        var stored: String? = null
        val settings = KeyValueAutoLockSettings(
            readStoredId = { stored },
            writeStoredId = { stored = it }
        )

        settings.setTimeout(AutoLockTimeout.FifteenMinutes)

        assertEquals(AutoLockTimeout.FifteenMinutes, settings.timeout.value)
        assertNotNull(stored)
        assertEquals(
            AutoLockTimeout.FifteenMinutes,
            KeyValueAutoLockSettings({ stored }, {}).timeout.value
        )
    }

    @Test
    fun backgroundingPastTheTimeoutClearsTheKeyWhileStillInTheBackground() = runTest {
        val session = unlockedSession()
        val controller = AutoLockController(
            session = session,
            settings = settingsOf(AutoLockTimeout.OneMinute),
            scope = backgroundScope,
            clock = { testScheduler.currentTime }
        )

        controller.onEnterBackground()
        testScheduler.advanceTimeBy(59_000)
        runCurrent()
        assertEquals(EncryptionSessionState.Unlocked, session.state.value)

        testScheduler.advanceTimeBy(1_001)
        runCurrent()

        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun aFrozenProcessThatNeverRanTheTimerLocksOnReturn() = runTest {
        val clock = MutableLockClock()
        val session = unlockedSession()
        val controller = AutoLockController(
            session = session,
            settings = settingsOf(AutoLockTimeout.OneMinute),
            scope = backgroundScope,
            clock = clock
        )

        controller.onEnterBackground()
        // Wall time passes; the process is not running, so the scheduler does not.
        clock.advanceBy(90_000)
        controller.onEnterForeground()

        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun returningWithinTheTimeoutResumesWithoutUnlocking() = runTest {
        val clock = MutableLockClock()
        val session = unlockedSession()
        val controller = AutoLockController(
            session = session,
            settings = settingsOf(AutoLockTimeout.FiveMinutes),
            scope = backgroundScope,
            clock = clock
        )

        controller.onEnterBackground()
        clock.advanceBy(120_000)
        testScheduler.advanceTimeBy(120_000)
        controller.onEnterForeground()
        // Well past the timeout in virtual time now: the cancelled timer must not fire later.
        testScheduler.advanceTimeBy(10 * 60 * 1000)
        runCurrent()

        assertEquals(EncryptionSessionState.Unlocked, session.state.value)
        assertNotNull(session.dataKeyOrNull())
    }

    @Test
    fun immediatelyLocksTheMomentTheAppLeavesTheForeground() = runTest {
        val session = unlockedSession()
        val controller = AutoLockController(
            session = session,
            settings = settingsOf(AutoLockTimeout.Immediately),
            scope = backgroundScope,
            clock = MutableLockClock()
        )

        controller.onEnterBackground()

        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun lockingDoesNotUndoSetupSoTheNextUnlockIsAnUnlockRatherThanASetup() = runTest {
        val session = unlockedSession()
        val controller = AutoLockController(
            session = session,
            settings = settingsOf(AutoLockTimeout.Immediately),
            scope = backgroundScope,
            clock = MutableLockClock()
        )

        controller.onEnterBackground()

        assertEquals(EncryptionSessionState.Locked, session.state.value)
    }

    private suspend fun unlockedSession(): InMemoryEncryptionSession =
        InMemoryEncryptionSession().apply { unlock(DataKey.generate()) }

    private fun settingsOf(timeout: AutoLockTimeout): AutoLockSettings =
        KeyValueAutoLockSettings(readStoredId = { timeout.storedId }, writeStoredId = {})
}
