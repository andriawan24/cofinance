package id.andriawan.cofinance.data.keyring

import id.andriawan.cofinance.data.crypto.DataKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** The three states the synchronization paths ask about, and the transitions between them. */
class EncryptionSessionTest {

    @Test
    fun aFreshSessionHasNoSetupAndNoKey() = runTest {
        val session = InMemoryEncryptionSession()

        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
        assertFalse(session.isSetUp)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun requiringTheKeyBeforeSetupFailsWithTheReason() = runTest {
        val session = InMemoryEncryptionSession()

        val failure = assertFailsWith<DataKeyUnavailableException> { session.requireDataKey() }

        assertEquals(EncryptionSessionState.SetupIncomplete, failure.sessionState)
    }

    @Test
    fun markingSetUpLeavesTheSessionLocked() = runTest {
        val session = InMemoryEncryptionSession().apply { markSetUp() }

        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertTrue(session.isSetUp)
        assertNull(session.dataKeyOrNull())
        assertEquals(
            EncryptionSessionState.Locked,
            assertFailsWith<DataKeyUnavailableException> { session.requireDataKey() }.sessionState
        )
    }

    @Test
    fun unlockingHoldsTheKeyInMemory() = runTest {
        val dataKey = DataKey.generate()
        val session = InMemoryEncryptionSession().apply { unlock(dataKey) }

        assertEquals(EncryptionSessionState.Unlocked, session.state.value)
        assertSame(dataKey, session.dataKeyOrNull())
        assertSame(dataKey, session.requireDataKey())
    }

    @Test
    fun lockingDropsTheKeyButKeepsSetup() = runTest {
        val session = InMemoryEncryptionSession().apply { unlock(DataKey.generate()) }

        session.lock()

        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertTrue(session.isSetUp)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun lockingBeforeSetupLeavesSetupIncomplete() = runTest {
        val session = InMemoryEncryptionSession()

        session.lock()

        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
    }

    @Test
    fun forgettingSetupReturnsToTheStartingState() = runTest {
        val session = InMemoryEncryptionSession().apply { unlock(DataKey.generate()) }

        session.forgetSetup()

        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun markingSetUpDoesNotRelockAnUnlockedSession() = runTest {
        val dataKey = DataKey.generate()
        val session = InMemoryEncryptionSession().apply { unlock(dataKey) }

        session.markSetUp()

        assertEquals(EncryptionSessionState.Unlocked, session.state.value)
        assertSame(dataKey, session.dataKeyOrNull())
    }

    @Test
    fun theStateFlowReportsEveryTransition() = runTest {
        val session = InMemoryEncryptionSession()
        val seen = mutableListOf(session.state.value)

        session.markSetUp().also { seen += session.state.value }
        session.unlock(DataKey.generate()).also { seen += session.state.value }
        session.lock().also { seen += session.state.value }

        assertEquals(
            listOf(
                EncryptionSessionState.SetupIncomplete,
                EncryptionSessionState.Locked,
                EncryptionSessionState.Unlocked,
                EncryptionSessionState.Locked
            ),
            seen
        )
    }
}
