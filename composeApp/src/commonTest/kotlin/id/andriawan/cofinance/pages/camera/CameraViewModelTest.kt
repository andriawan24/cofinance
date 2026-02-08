package id.andriawan.cofinance.pages.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CameraViewModelTest {

    @Test
    fun testInitialUiState() {
        val viewModel = CameraViewModel()
        val uiState = viewModel.uiState.value

        assertFalse(uiState.flashlightOn, "Flashlight should be off initially")
        assertFalse(uiState.shouldShowRationalDialog, "Rational dialog should not be shown initially")
        assertFalse(uiState.showLoading, "Loading should be false initially")
    }

    @Test
    fun testCameraUiStateDefaults() {
        val uiState = CameraUiState()

        assertFalse(uiState.flashlightOn)
        assertFalse(uiState.shouldShowRationalDialog)
        assertFalse(uiState.showLoading)
    }

    @Test
    fun testCameraUiStateWithCustomValues() {
        val uiState = CameraUiState(
            flashlightOn = true,
            shouldShowRationalDialog = true,
            showLoading = true
        )

        assertTrue(uiState.flashlightOn)
        assertTrue(uiState.shouldShowRationalDialog)
        assertTrue(uiState.showLoading)
    }

    @Test
    fun testCameraUiStateCopy() {
        val original = CameraUiState(flashlightOn = false)
        val modified = original.copy(flashlightOn = true)

        assertFalse(original.flashlightOn)
        assertTrue(modified.flashlightOn)
        assertEquals(original.shouldShowRationalDialog, modified.shouldShowRationalDialog)
        assertEquals(original.showLoading, modified.showLoading)
    }

    @Test
    fun testCameraUiStateIndependence() {
        val state1 = CameraUiState(flashlightOn = true)
        val state2 = CameraUiState(flashlightOn = false)

        assertTrue(state1.flashlightOn)
        assertFalse(state2.flashlightOn)
    }

    @Test
    fun testViewModelHasUiStateFlow() {
        val viewModel = CameraViewModel()
        assertNotNull(viewModel.uiState)
    }

    @Test
    fun testViewModelHasUiEventFlow() {
        val viewModel = CameraViewModel()
        assertNotNull(viewModel.uiEvent)
    }

    @Test
    fun testCameraUiEventShowError() {
        val message = "Failed to capture image"
        val event = CameraUiEvent.ShowError(message)

        assertTrue(event is CameraUiEvent.ShowError)
        assertEquals(message, event.message)
    }

    @Test
    fun testCameraUiEventShowErrorWithEmptyMessage() {
        val event = CameraUiEvent.ShowError("")
        assertTrue(event is CameraUiEvent.ShowError)
        assertEquals("", event.message)
    }

    @Test
    fun testCameraUiEventShowErrorWithLongMessage() {
        val longMessage = "A very detailed error message that explains what went wrong during the image capture process, including technical details about the failure"
        val event = CameraUiEvent.ShowError(longMessage)

        assertTrue(event is CameraUiEvent.ShowError)
        assertEquals(longMessage, event.message)
        assertTrue(event.message.length > 100)
    }

    @Test
    fun testCameraUiEventExhaustiveCheck() {
        val error = CameraUiEvent.ShowError("test")

        when (error) {
            is CameraUiEvent.ShowError -> assertTrue(error.message.isNotEmpty())
            is CameraUiEvent.ImageCaptured -> assertTrue(false, "Should not reach ImageCaptured case")
        }
    }

    @Test
    fun testMultipleViewModelInstances() {
        val viewModel1 = CameraViewModel()
        val viewModel2 = CameraViewModel()

        assertNotNull(viewModel1.uiState)
        assertNotNull(viewModel2.uiState)

        // Each instance should have its own state
        assertFalse(viewModel1.uiState.value.flashlightOn)
        assertFalse(viewModel2.uiState.value.flashlightOn)
    }

    @Test
    fun testCameraUiStateFieldMutability() {
        val state = CameraUiState()
        // Test that var field can be modified
        state.showLoading = true
        assertTrue(state.showLoading)

        state.showLoading = false
        assertFalse(state.showLoading)
    }

    @Test
    fun testCameraUiStateAllFieldsCombinations() {
        val allTrue = CameraUiState(
            flashlightOn = true,
            shouldShowRationalDialog = true,
            showLoading = true
        )
        assertTrue(allTrue.flashlightOn)
        assertTrue(allTrue.shouldShowRationalDialog)
        assertTrue(allTrue.showLoading)

        val allFalse = CameraUiState(
            flashlightOn = false,
            shouldShowRationalDialog = false,
            showLoading = false
        )
        assertFalse(allFalse.flashlightOn)
        assertFalse(allFalse.shouldShowRationalDialog)
        assertFalse(allFalse.showLoading)

        val mixed = CameraUiState(
            flashlightOn = true,
            shouldShowRationalDialog = false,
            showLoading = true
        )
        assertTrue(mixed.flashlightOn)
        assertFalse(mixed.shouldShowRationalDialog)
        assertTrue(mixed.showLoading)
    }
}