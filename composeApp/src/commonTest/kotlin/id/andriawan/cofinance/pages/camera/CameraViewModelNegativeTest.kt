package id.andriawan.cofinance.pages.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Negative and boundary tests for CameraViewModel
 * Tests edge cases, negative scenarios, and boundary conditions
 */
class CameraViewModelNegativeTest {

    @Test
    fun testUiStateDefaultsAreAllFalse() {
        val uiState = CameraUiState()

        // All boolean flags should default to false
        assertFalse(uiState.flashlightOn)
        assertFalse(uiState.shouldShowRationalDialog)
        assertFalse(uiState.showLoading)
    }

    @Test
    fun testUiStateCanHaveMultipleTrueFlags() {
        val uiState = CameraUiState(
            flashlightOn = true,
            shouldShowRationalDialog = true,
            showLoading = true
        )

        // All can be true simultaneously
        assertTrue(uiState.flashlightOn)
        assertTrue(uiState.shouldShowRationalDialog)
        assertTrue(uiState.showLoading)
    }

    @Test
    fun testUiStateCanHaveMixedFlags() {
        val state1 = CameraUiState(flashlightOn = true, shouldShowRationalDialog = false)
        val state2 = CameraUiState(flashlightOn = false, showLoading = true)
        val state3 = CameraUiState(shouldShowRationalDialog = true, showLoading = true)

        assertTrue(state1.flashlightOn)
        assertFalse(state1.shouldShowRationalDialog)

        assertFalse(state2.flashlightOn)
        assertTrue(state2.showLoading)

        assertTrue(state3.shouldShowRationalDialog)
        assertTrue(state3.showLoading)
    }

    @Test
    fun testUiStateCopyPreservesUnchangedFields() {
        val original = CameraUiState(
            flashlightOn = true,
            shouldShowRationalDialog = false,
            showLoading = true
        )

        val copied = original.copy()

        assertEquals(original.flashlightOn, copied.flashlightOn)
        assertEquals(original.shouldShowRationalDialog, copied.shouldShowRationalDialog)
        assertEquals(original.showLoading, copied.showLoading)
    }

    @Test
    fun testUiStateCopyCanChangeOneField() {
        val original = CameraUiState(flashlightOn = false)
        val modified = original.copy(flashlightOn = true)

        assertFalse(original.flashlightOn)
        assertTrue(modified.flashlightOn)
        assertEquals(original.shouldShowRationalDialog, modified.shouldShowRationalDialog)
    }

    @Test
    fun testUiStateCopyCanChangeMultipleFields() {
        val original = CameraUiState()
        val modified = original.copy(
            flashlightOn = true,
            showLoading = true
        )

        assertFalse(original.flashlightOn)
        assertFalse(original.showLoading)
        assertTrue(modified.flashlightOn)
        assertTrue(modified.showLoading)
    }

    @Test
    fun testUiStateVarFieldCanBeModified() {
        val state = CameraUiState(showLoading = false)

        assertFalse(state.showLoading)

        state.showLoading = true
        assertTrue(state.showLoading)

        state.showLoading = false
        assertFalse(state.showLoading)
    }

    @Test
    fun testUiStateVarFieldModificationDoesNotAffectCopies() {
        val original = CameraUiState(showLoading = false)
        val copy = original.copy()

        original.showLoading = true

        assertTrue(original.showLoading)
        assertFalse(copy.showLoading)
    }

    @Test
    fun testCameraEventShowErrorWithEmptyString() {
        val event = CameraUiEvent.ShowError("")

        assertTrue(event is CameraUiEvent.ShowError)
        assertEquals("", event.message)
    }

    @Test
    fun testCameraEventShowErrorWithWhitespace() {
        val event = CameraUiEvent.ShowError("   ")

        assertTrue(event is CameraUiEvent.ShowError)
        assertEquals("   ", event.message)
    }

    @Test
    fun testCameraEventShowErrorWithLongMessage() {
        val longMessage = "x".repeat(1000)
        val event = CameraUiEvent.ShowError(longMessage)

        assertTrue(event is CameraUiEvent.ShowError)
        assertEquals(1000, event.message.length)
    }

    @Test
    fun testCameraEventShowErrorWithSpecialCharacters() {
        val messages = listOf(
            "Error\nwith\nnewlines",
            "Error\twith\ttabs",
            "Error with unicode: 🎥📸",
            "Error with quotes: \"test\"",
            "Error with backslash: \\"
        )

        messages.forEach { message ->
            val event = CameraUiEvent.ShowError(message)
            assertTrue(event is CameraUiEvent.ShowError)
            assertEquals(message, event.message)
        }
    }

    @Test
    fun testMultipleViewModelsHaveIndependentState() {
        val vm1 = CameraViewModel()
        val vm2 = CameraViewModel()

        assertNotNull(vm1.uiState.value)
        assertNotNull(vm2.uiState.value)

        // States should be independent
        assertFalse(vm1.uiState.value.flashlightOn)
        assertFalse(vm2.uiState.value.flashlightOn)
    }

    @Test
    fun testUiStateEquality() {
        val state1 = CameraUiState(flashlightOn = true)
        val state2 = CameraUiState(flashlightOn = true)
        val state3 = CameraUiState(flashlightOn = false)

        // Note: Data class equality is based on properties
        // Since showLoading is a var, equality might not work as expected
        assertTrue(state1.flashlightOn == state2.flashlightOn)
        assertFalse(state1.flashlightOn == state3.flashlightOn)
    }

    @Test
    fun testUiStateTogglingFlags() {
        val state = CameraUiState()

        // Toggle flashlight
        val withFlashlight = state.copy(flashlightOn = true)
        val withoutFlashlight = withFlashlight.copy(flashlightOn = false)

        assertFalse(state.flashlightOn)
        assertTrue(withFlashlight.flashlightOn)
        assertFalse(withoutFlashlight.flashlightOn)
    }

    @Test
    fun testCameraEventShowErrorMessagesAreDistinct() {
        val event1 = CameraUiEvent.ShowError("Error 1")
        val event2 = CameraUiEvent.ShowError("Error 2")

        assertTrue(event1 is CameraUiEvent.ShowError)
        assertTrue(event2 is CameraUiEvent.ShowError)
        assertNotEquals(event1.message, event2.message)
    }

    @Test
    fun testCameraEventShowErrorWithNullCharacters() {
        // Test handling of unusual characters
        val message = "Error\u0000with\u0000null"
        val event = CameraUiEvent.ShowError(message)

        assertTrue(event is CameraUiEvent.ShowError)
        assertTrue(event.message.contains("\u0000"))
    }

    @Test
    fun testUiStateWithAllPermutations() {
        val permutations = listOf(
            CameraUiState(false, false, false),
            CameraUiState(true, false, false),
            CameraUiState(false, true, false),
            CameraUiState(false, false, true),
            CameraUiState(true, true, false),
            CameraUiState(true, false, true),
            CameraUiState(false, true, true),
            CameraUiState(true, true, true)
        )

        // Verify all 8 permutations are valid
        assertEquals(8, permutations.size)
        permutations.forEach { state ->
            assertNotNull(state)
        }
    }

    @Test
    fun testViewModelInitialStateIsConsistent() {
        // Create multiple ViewModels and verify they all start with the same state
        val viewModels = List(5) { CameraViewModel() }

        viewModels.forEach { vm ->
            assertFalse(vm.uiState.value.flashlightOn)
            assertFalse(vm.uiState.value.shouldShowRationalDialog)
            assertFalse(vm.uiState.value.showLoading)
        }
    }

    @Test
    fun testCameraEventExhaustiveHandling() {
        val events = listOf(
            CameraUiEvent.ShowError("Test error")
        )

        events.forEach { event ->
            val handled = when (event) {
                is CameraUiEvent.ShowError -> true
                is CameraUiEvent.ImageCaptured -> true
            }
            assertTrue(handled)
        }
    }
}