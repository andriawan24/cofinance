package id.andriawan.cofinance.pages.camera

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import coil3.Uri
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow


data class CameraUiState(
    val flashlightOn: Boolean = false,
    val shouldShowRationalDialog: Boolean = false,
    var showLoading: Boolean = false
)

sealed class CameraUiEvent {
    data class ShowError(val message: String) : CameraUiEvent()
    data class ImageCaptured(val imageUri: Uri) : CameraUiEvent()
}


@Immutable
class CameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<CameraUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

//    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
//    val surfaceRequest = _surfaceRequest.asStateFlow()
//
//    private var camera: Camera? = null
//    private var imageCapture: ImageCapture? = null
//
//    private val cameraPreviewUseCase = Preview.Builder().build().apply {
//        setSurfaceProvider { newCameraRequest ->
//            _surfaceRequest.update { newCameraRequest }
//        }
//    }
//
//    fun showRationaleDialog(isShowing: Boolean) {
//        _uiState.update { it.copy(shouldShowRationalDialog = isShowing) }
//    }
//
//    fun captureImage(appContext: Context) {
//        viewModelScope.launch {
//            _uiState.update { it.copy(showLoading = true) }
//
//            val executor = ContextCompat.getMainExecutor(appContext)
//            val receiptDirectory = File(appContext.cacheDir, "receipts")
//            if (!receiptDirectory.exists()) receiptDirectory.mkdirs()
//            val newReceiptFile = File(receiptDirectory, "${UUID.randomUUID()}.jpg")
//
//            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(newReceiptFile).build()
//
//            imageCapture?.takePicture(
//                outputFileOptions,
//                executor,
//                object : ImageCapture.OnImageSavedCallback {
//                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                        outputFileResults.savedUri?.let { imageUri ->
//                            viewModelScope.launch {
//                                _uiState.update { it.copy(showLoading = false) }
//                                _uiEvent.send(CameraUiEvent.ImageCaptured(imageUri))
//                            }
//                        }
//                    }
//
//                    override fun onError(exception: ImageCaptureException) {
//                        viewModelScope.launch {
//                            _uiState.update { it.copy(showLoading = false) }
//                            _uiEvent.send(CameraUiEvent.ShowError(exception.message.orEmpty()))
//                        }
//                    }
//                }
//            )
//        sdsSD}
//    }
//
//    fun toggleFlashlight() {
//        if (camera?.cameraInfo?.hasFlashUnit() == true) {
//            _uiState.update { it.copy(flashlightOn = !it.flashlightOn) }
//            camera?.cameraControl?.enableTorch(uiState.value.flashlightOn)
//        }
//    }
//
//    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
//        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
//        val cameraSelector = CameraSelector.Builder()
//            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//            .build()
//
//        imageCapture = ImageCapture.Builder().build()
//        camera = processCameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            cameraSelector,
//            imageCapture,
//            cameraPreviewUseCase
//        )
//
//        try {
//            awaitCancellation()
//        } finally {
//            processCameraProvider.unbindAll()
//        }
//    }
}
