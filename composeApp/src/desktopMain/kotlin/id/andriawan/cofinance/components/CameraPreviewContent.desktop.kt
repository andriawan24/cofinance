package id.andriawan.cofinance.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import kotlinx.coroutines.launch

@Composable
actual fun CameraPreviewContent(
    modifier: Modifier,
    onCaptureImage: (filePath: String) -> Unit,
    onFlashToggled: (isFlashOn: Boolean) -> Unit,
    cameraActions: CameraActions?
) {
    val scope = rememberCoroutineScope()

    val cameraState by rememberCameraKState(
        config = CameraConfiguration(
            cameraLens = CameraLens.BACK,
            flashMode = FlashMode.OFF
        )
    )

    CameraKScreen(
        cameraState = cameraState,
        modifier = modifier.fillMaxSize(),
        loadingContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        errorContent = { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera error: ${error.message}")
            }
        }
    ) { readyState ->
        LaunchedEffect(readyState) {
            cameraActions?.captureAction = {
                scope.launch {
                    when (val result = readyState.controller.takePictureToFile()) {
                        is ImageCaptureResult.SuccessWithFile -> {
                            onCaptureImage(result.filePath)
                        }
                        is ImageCaptureResult.Success -> {}
                        is ImageCaptureResult.Error -> {}
                    }
                }
            }
            cameraActions?.flashToggleAction = {
                readyState.controller.toggleFlashMode()
                val flashMode = readyState.controller.getFlashMode()
                onFlashToggled(flashMode != FlashMode.OFF)
            }
        }
    }
}
