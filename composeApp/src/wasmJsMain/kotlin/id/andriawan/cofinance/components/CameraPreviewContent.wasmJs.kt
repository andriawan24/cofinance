package id.andriawan.cofinance.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun CameraPreviewContent(
    modifier: Modifier,
    onCaptureImage: (filePath: String) -> Unit,
    onFlashToggled: (isFlashOn: Boolean) -> Unit,
    cameraActions: CameraActions?
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Camera is not supported on this platform")
    }
}
