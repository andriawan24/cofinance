package id.andriawan.cofinance.pages.encryption

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.remote.FakeFinanceDocumentStore
import id.andriawan.cofinance.data.remote.FinanceCollection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Restoring onto a device that holds no key material, with a real phrase wrap published by a
 * simulated previous device.
 *
 * Every test here starts from a backend seeded through the production paths — the key material gate
 * publishes a real recovery-phrase wrap, and the encrypted data sources upload real ciphertext — so
 * "the phrase opens it" is a fact about the wrapping layer rather than about a fixture. The failure
 * cases then assert on what was imported, which is the only claim that matters: a phrase that does
 * not open the material must leave the local database exactly as empty as it found it.
 */
class RecoveryPhraseRestoreViewModelTest {

    @Test
    fun aCleanInstallRestoreImportsTheSynchronizedRecords() = runTest {
        val seeded = seedBackend()
        val fixture = EncryptionFixture(store = seeded.store)
        val viewModel = fixture.restoreViewModel()

        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(seeded.phrase.text))
        viewModel.restore()

        val state = viewModel.uiState.value
        assertTrue(state.isRestored, "Restore did not complete: ${state.error}")
        assertNull(state.error)
        assertEquals(1, state.importedAccounts)
        assertEquals(1, state.importedTransactions)

        // The values themselves came back, not just the row count.
        assertEquals(listOf(SEEDED_ACCOUNT), fixture.localAccounts.getAccounts())
        assertEquals(
            listOf(SEEDED_TRANSACTION),
            fixture.localTransactions.getAllTransactions()
        )
    }

    @Test
    fun aSuccessfulRestoreLeavesADeviceBoundWrappedCopyBehind() = runTest {
        val seeded = seedBackend()
        val fixture = EncryptionFixture(store = seeded.store)
        val viewModel = fixture.restoreViewModel()

        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(seeded.phrase.text))
        viewModel.restore()

        assertEquals(EncryptionSessionState.Unlocked, fixture.session.state.value)

        val local = assertNotNull(fixture.localKeyMaterialStore.read())
        val deviceWrap = local.wrapsOf(KeyWrapType.Device).singleOrNull()
        assertNotNull(deviceWrap, "No device-bound copy was created, so the phrase is still required")

        // The new copy opens on this device, which is what makes the next launch phrase-free.
        val reopened = fixture.deviceKeyWrapper.unwrap(deviceWrap)
        assertEquals(seeded.dataKey.id, reopened.id)

        // And it stayed local: the backend still holds only the phrase wrap.
        val uploaded = fixture.store.storedKeyMaterialText()
        assertTrue(KeyWrapType.Device.id !in uploaded, "The device wrap was uploaded")
    }

    @Test
    fun anInvalidPhraseImportsNothingAndPermitsRetry() = runTest {
        val seeded = seedBackend()
        val fixture = EncryptionFixture(store = seeded.store)
        val viewModel = fixture.restoreViewModel()

        // A well-formed phrase that simply is not this account's.
        val unrelated = RecoveryPhrase.generate()
        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(unrelated.text))
        viewModel.restore()

        assertEquals(
            RecoveryPhraseEntryError.PhraseDoesNotOpenThisAccount,
            viewModel.uiState.value.error
        )
        assertTrue(fixture.localAccounts.getAccounts().isEmpty(), "A wrong phrase imported accounts")
        assertTrue(fixture.localTransactions.getAllTransactions().isEmpty())
        assertEquals(
            EncryptionSessionState.SetupIncomplete,
            fixture.session.state.value,
            "A wrong phrase unlocked the session"
        )
        assertNull(fixture.localKeyMaterialStore.read())
        assertTrue(viewModel.uiState.value.canRestore, "Retry was not offered")

        // Retry with the right phrase on the same view model.
        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(seeded.phrase.text))
        viewModel.restore()

        assertTrue(viewModel.uiState.value.isRestored)
        assertEquals(listOf(SEEDED_ACCOUNT), fixture.localAccounts.getAccounts())
    }

    @Test
    fun aMisspelledWordIsReportedAtItsPosition() = runTest {
        val seeded = seedBackend()
        val fixture = EncryptionFixture(store = seeded.store)
        val viewModel = fixture.restoreViewModel()

        val words = seeded.phrase.words.toMutableList()
        words[4] = "abandonn"
        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(words.joinToString(" ")))
        viewModel.restore()

        assertEquals(
            RecoveryPhraseEntryError.UnknownWord(position = 5, word = "abandonn"),
            viewModel.uiState.value.error
        )
        assertTrue(fixture.localAccounts.getAccounts().isEmpty())
    }

    @Test
    fun aPhraseOfRealWordsThatDoesNotCheckOutIsReportedAsSuchRatherThanAsAMisspelling() = runTest {
        val seeded = seedBackend()
        val fixture = EncryptionFixture(store = seeded.store)
        val viewModel = fixture.restoreViewModel()

        // The published all-zero-entropy vector ends in "about", which encodes the checksum. Twelve
        // "abandon"s are all real words with a checksum of zero, which is not the one the entropy
        // demands — exactly what a swapped or transposed word looks like to the parser.
        val allRealWords = List(RecoveryPhrase.WORD_COUNT) { "abandon" }.joinToString(" ")
        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(allRealWords))
        viewModel.restore()

        assertEquals(RecoveryPhraseEntryError.ChecksumFailed, viewModel.uiState.value.error)
        assertTrue(fixture.localAccounts.getAccounts().isEmpty())
    }

    @Test
    fun anIncompletePhraseIsReportedByHowManyWordsArrived() = runTest {
        val seeded = seedBackend()
        val fixture = EncryptionFixture(store = seeded.store)
        val viewModel = fixture.restoreViewModel()

        viewModel.onEvent(
            RecoveryPhraseRestoreUiEvent.PhraseChanged(
                seeded.phrase.words.take(6).joinToString("\n")
            )
        )
        viewModel.restore()

        assertEquals(RecoveryPhraseEntryError.WrongWordCount(actual = 6), viewModel.uiState.value.error)
        assertTrue(fixture.localAccounts.getAccounts().isEmpty())
    }

    @Test
    fun anAccountWithNoStoredKeyMaterialSaysSoRatherThanBlamingThePhrase() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.restoreViewModel()

        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(RecoveryPhrase.generate().text))
        viewModel.restore()

        assertEquals(RecoveryPhraseEntryError.NoStoredKeyMaterial, viewModel.uiState.value.error)
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
    }

    @Test
    fun aDownloadThatFailsPartWayImportsNothingAndCanBeRetried() = runTest {
        val seeded = seedBackend()
        val flaky = FlakyFinanceDocumentStore(seeded.store)
        val fixture = EncryptionFixture(store = seeded.store, documentStore = flaky)
        val viewModel = fixture.restoreViewModel()

        // Accounts read fine, transactions do not. This is the case where a naive implementation
        // leaves half the records imported.
        flaky.failReadsFrom = FinanceCollection.TRANSACTIONS
        viewModel.onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(seeded.phrase.text))
        viewModel.restore()

        assertEquals(RecoveryPhraseEntryError.RestoreFailed, viewModel.uiState.value.error)
        assertTrue(
            fixture.localAccounts.getAccounts().isEmpty(),
            "Accounts were imported even though the transactions never arrived"
        )
        assertTrue(fixture.localTransactions.getAllTransactions().isEmpty())
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
        assertNull(fixture.localKeyMaterialStore.read())

        flaky.failReadsFrom = null
        viewModel.restore()

        assertTrue(viewModel.uiState.value.isRestored)
        assertEquals(listOf(SEEDED_ACCOUNT), fixture.localAccounts.getAccounts())
        assertEquals(listOf(SEEDED_TRANSACTION), fixture.localTransactions.getAllTransactions())
    }
}

private class SeededBackend(
    val store: FakeFinanceDocumentStore,
    val phrase: RecoveryPhrase,
    val dataKey: DataKey
)

/**
 * Stands up the state a previous device would have left behind: a real recovery-phrase wrap in the
 * backend, and one encrypted account and transaction under the same data key.
 */
private suspend fun seedBackend(): SeededBackend {
    val dataKey = DataKey.generate()
    val phrase = RecoveryPhrase.generate()
    val fixture = EncryptionFixture()
    fixture.session.unlock(dataKey)

    fixture.keyMaterialGate.publishKeyMaterial(
        KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                fixture.deviceKeyWrapper.wrap(dataKey),
                fixture.recoveryPhraseKeyWrapper.wrap(dataKey, phrase)
            )
        )
    )
    fixture.remoteAccounts.upsertAccounts(listOf(SEEDED_ACCOUNT))
    fixture.remoteTransactions.upsertTransactions(listOf(SEEDED_TRANSACTION))

    return SeededBackend(fixture.store, phrase, dataKey)
}
