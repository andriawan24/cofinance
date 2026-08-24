package id.andriawan.cofinance.pages.encryption

import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PhraseExportStatus
import id.andriawan.cofinance.data.crypto.RECOVERY_PHRASE_FILE_NAME
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.toRecoveryPhraseExportText
import id.andriawan.cofinance.data.keyring.DataKeyUnavailableException
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.model.AccountResponse
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
 * What setup does, and what it refuses to do before the user has moved past the phrase.
 *
 * Setup no longer examines the phrase — the three-word quiz is gone, since the words were on the
 * screen above it — so what these tests pin is the part that always mattered: nothing is published
 * and nothing synchronizes until the user leaves the phrase screen, and the copy and save actions
 * hand over the phrase without becoming a condition of finishing.
 */
class EncryptionSetupViewModelTest {

    @Test
    fun thePhraseIsShownWithoutAnyWordsBeingAskedBack() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()

        val state = viewModel.uiState.value
        assertEquals(EncryptionSetupStep.PhraseDisplay, state.step)
        assertEquals(RecoveryPhrase.WORD_COUNT, state.words.size)
        assertNull(state.exportStatus)
    }

    @Test
    fun synchronizationCannotBeginWhileThePhraseIsStillOnScreen() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()

        // The encrypted source is the only route finance data has out of the device, and without a
        // data key it refuses rather than falling back to anything readable.
        assertFailsWith<DataKeyUnavailableException> {
            fixture.remoteAccounts.upsertAccounts(listOf(AccountResponse(id = "a-1", name = "Cash")))
        }
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
        assertTrue(fixture.store.writeLog.isEmpty(), "A record reached the backend before setup")
        assertTrue(fixture.store.documentIds(FinanceCollection.ACCOUNTS).isEmpty())
        assertNull(fixture.localKeyMaterialStore.read())
    }

    @Test
    fun copyingOrSavingThePhraseDoesNotCompleteSetupByItself() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        viewModel.copyPhrase()
        viewModel.downloadPhrase()

        assertEquals(EncryptionSetupStep.PhraseDisplay, viewModel.uiState.value.step)
        assertEquals(EncryptionSessionState.SetupIncomplete, fixture.session.state.value)
        assertTrue(fixture.store.writeLog.isEmpty())
    }

    @Test
    fun theCopiedAndSavedPhraseIsTheOneOnScreen() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        val expected = viewModel.uiState.value.words.toRecoveryPhraseExportText()

        viewModel.copyPhrase()
        assertEquals(expected, fixture.recoveryPhraseExporter.copiedText)
        assertEquals(PhraseExportStatus.Copied, viewModel.uiState.value.exportStatus)

        viewModel.downloadPhrase()
        assertEquals(expected, fixture.recoveryPhraseExporter.savedText)
        assertEquals(RECOVERY_PHRASE_FILE_NAME, fixture.recoveryPhraseExporter.savedFileName)
        assertEquals(
            PhraseExportStatus.Saved("Download/cofinance-recovery-phrase.txt"),
            viewModel.uiState.value.exportStatus
        )
    }

    @Test
    fun anExportThatDidNotHappenIsReportedAsAFailureRatherThanAsSuccess() = runTest {
        val fixture = EncryptionFixture()
        fixture.recoveryPhraseExporter.copySucceeds = false
        // What a cancelled save picker looks like from here: no location, so no file.
        fixture.recoveryPhraseExporter.savedLocation = null
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()

        viewModel.copyPhrase()
        assertEquals(PhraseExportStatus.CopyFailed, viewModel.uiState.value.exportStatus)

        viewModel.downloadPhrase()
        assertEquals(PhraseExportStatus.SaveFailed, viewModel.uiState.value.exportStatus)

        // A failed export is a notice, not a setup failure: the phrase is still on screen and the
        // user can still finish by writing it down.
        assertNull(viewModel.uiState.value.error)
        viewModel.finishSetup()
        assertEquals(EncryptionSetupStep.Completed, viewModel.uiState.value.step)
    }

    @Test
    fun leavingThePhraseScreenCompletesSetupAndUnlocksTheSession() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        viewModel.finishSetup()

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
        viewModel.finishSetup()

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
    fun theStoredPhraseIsTheOneTheUserWasShown() = runTest {
        val fixture = EncryptionFixture()
        val viewModel = fixture.setupViewModel()

        viewModel.prepare()
        val words = viewModel.uiState.value.words
        viewModel.finishSetup()

        val key = assertNotNull(fixture.session.dataKeyOrNull())
        val kept = assertNotNull(fixture.recoveryPhraseVault.read(key))
        assertEquals(words, kept.words)
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
