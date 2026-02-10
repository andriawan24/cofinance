package id.andriawan.cofinance.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPreviewContent(
    modifier: Modifier = Modifier,
    onCaptureImage: (filePath: String) -> Unit = {},
    onFlashToggled: (isFlashOn: Boolean) -> Unit = {},
    cameraActions: CameraActions? = null
)

@Stable
class CameraActions {
    internal var captureAction: (() -> Unit)? = null
    internal var flashToggleAction: (() -> Unit)? = null

    fun capture() {
        captureAction?.invoke()
    }

    fun toggleFlash() {
        flashToggleAction?.invoke()
    }
}
