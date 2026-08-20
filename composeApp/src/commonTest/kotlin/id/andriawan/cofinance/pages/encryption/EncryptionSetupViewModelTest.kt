package id.andriawan.cofinance.pages.encryption

import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.keyring.DataKeyUnavailableException
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.remote.FakeFinanceDocumentStore
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.keyMaterialWith
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * What setup does, and — more importantly — what it refuses to do before the requested words come
 * back correctly.
 *
 * The verification this task names has two halves, and both are asserted against something
 * observable rather than against the view model's own opinion: setup does not complete, which is the
 * session still reporting `SetupIncomplete`, and synchronization does not begin, which is an
 * encrypted write refusing to run and the fake store's write log staying empty.
 */
class EncryptionSetupViewModelTest {

    @Test
    fun theRequestedWordsAreDrawnFromThePhraseRatherThanBeingTheWholePhrase() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)

        val requested = viewModel.uiState.value.requestedWords
        assertEquals(EncryptionSetupViewModel.REQUESTED_WORD_COUNT, requested.size)
        assertTrue(
            requested.size < RecoveryPhrase.WORD_COUNT,
            "Confirmation asked for the whole phrase, which copying from the screen would satisfy"
        )
        assertEquals(
            requested.map { it.position }.distinct().size,
            requested.size,
            "The same position was requested twice"
        )
        assertTrue(requested.all { it.position in 1..RecoveryPhrase.WORD_COUNT })
    }

    @Test
    fun setupDoesNotCompleteUntilTheRequestedWordsAreCorrect() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)
        viewModel.answerAll { "abandon-not-a-word" }
        viewModel.confirm()

        val state = viewModel.uiState.value
        assertEquals(EncryptionSetupStep.Confirmation, state.step, "Setup completed on wrong words")
        assertEquals(EncryptionSetupError.RequestedWordsDoNotMatch, state.error)
        assertTrue(state.requestedWords.all { it.isWrong })
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
        assertTrue(fixture.store.writeLog.isEmpty(), "Key material was published before confirmation")
        assertNull(fixture.localKeyMaterialStore.read())
    }

    @Test
    fun synchronizationCannotBeginBeforeTheRequestedWordsAreCorrect() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)
        viewModel.answerAll { "wrong" }
        viewModel.confirm()

        // The encrypted source is the only route finance data has out of the device, and without a
        // data key it refuses rather than falling back to anything readable.
        assertFailsWith<DataKeyUnavailableException> {
            fixture.remoteAccounts.upsertAccounts(listOf(AccountResponse(id = "a-1", name = "Cash")))
        }
        assertTrue(fixture.store.writeLog.isEmpty(), "A record reached the backend before setup")
        assertTrue(fixture.store.documentIds(FinanceCollection.ACCOUNTS).isEmpty())
    }

    @Test
    fun correctWordsCompleteSetupAndUnlockTheSession() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        val words = viewModel.uiState.value.words
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)
        viewModel.answerCorrectly(words)
        viewModel.confirm()

        assertEquals(EncryptionSetupStep.Completed, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.error)
        assertEquals(EncryptionSessionState.Unlocked, fixture.session.state.value)
        assertNotNull(fixture.session.dataKeyOrNull())
    }

    @Test
    fun onlyTheRecoveryPhraseWrapIsPublishedAndItPrecedesEveryRecord() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        val words = viewModel.uiState.value.words
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)
        viewModel.answerCorrectly(words)
        viewModel.confirm()

        assertEquals(
            listOf(FakeFinanceDocumentStore.KEY_MATERIAL_ENTRY),
            fixture.store.writeLog,
            "Setup wrote something other than key material, or wrote it in the wrong order"
        )

        val uploaded = fixture.store.storedKeyMaterialText()
        assertContains(uploaded, KeyWrapType.RecoveryPhrase.id)
        assertFalse(uploaded.contains(KeyWrapType.Device.id), "The device wrap left the device")

        // The device wrap is the copy that opens the data without the phrase, so it has to be held
        // locally even though it is never uploaded.
        val local = assertNotNull(fixture.localKeyMaterialStore.read())
        assertEquals(1, local.wrapsOf(KeyWrapType.Device).size)
        assertEquals(1, local.wrapsOf(KeyWrapType.RecoveryPhrase).size)

        fixture.remoteAccounts.upsertAccounts(listOf(AccountResponse(id = "a-1", name = "Cash")))
        assertEquals(
            listOf(FakeFinanceDocumentStore.KEY_MATERIAL_ENTRY, "accounts/a-1"),
            fixture.store.writeLog
        )
    }

    @Test
    fun aMistypedWordCanBeCorrectedAndSetupThenCompletes() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        val words = viewModel.uiState.value.words
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)
        viewModel.answerAll { "wrong" }
        viewModel.confirm()
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)

        viewModel.answerCorrectly(words)
        viewModel.confirm()

        assertEquals(EncryptionSetupStep.Completed, viewModel.uiState.value.step)
        assertEquals(EncryptionSessionState.Unlocked, fixture.session.state.value)
    }

    @Test
    fun aWordIsAcceptedWithTheCapitalizationAndSpacingAKeyboardAdds() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        val words = viewModel.uiState.value.words
        viewModel.onEvent(EncryptionSetupUiEvent.PhraseWrittenDown)
        viewModel.uiState.value.requestedWords.forEach { requested ->
            viewModel.onEvent(
                EncryptionSetupUiEvent.RequestedWordChanged(
                    position = requested.position,
                    value = " ${words[requested.position - 1].replaceFirstChar(Char::uppercase)} "
                )
            )
        }
        viewModel.confirm()

        assertEquals(EncryptionSetupStep.Completed, viewModel.uiState.value.step)
    }

    @Test
    fun anAccountThatAlreadyHasKeyMaterialIsSentToRestoreRatherThanGivenANewPhrase() = runTest {
        val fixture = EncryptionFixture()
        fixture.store.seedKeyMaterial(keyMaterialWith("key-1", KeyWrapType.RecoveryPhrase))
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()

        assertEquals(EncryptionSetupStep.RestoreRequired, viewModel.uiState.value.step)
        assertTrue(
            viewModel.uiState.value.words.isEmpty(),
            "A second phrase was generated, which would strand the records under the first one"
        )
        assertTrue(fixture.store.writeLog.isEmpty())
    }
}

/** Fills every requested word with whatever [answer] returns for its position. */
private fun EncryptionSetupViewModel.answerAll(answer: (Int) -> String) {
    uiState.value.requestedWords.forEach { requested ->
        onEvent(
            EncryptionSetupUiEvent.RequestedWordChanged(
                position = requested.position,
                value = answer(requested.position)
            )
        )
    }
}

/** Fills every requested word with the word the phrase actually holds at that position. */
private fun EncryptionSetupViewModel.answerCorrectly(words: List<String>) =
    answerAll { position -> words[position - 1] }
