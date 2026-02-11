package id.andriawan.cofinance.pages.camera

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.andriawan.cofinance.components.CameraActions
import id.andriawan.cofinance.components.CameraContent
import id.andriawan.cofinance.components.CameraPreviewContent
import id.andriawan.cofinance.components.rememberGalleryLauncher

@Composable
fun CameraScreen(
    onNavigateToPreview: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    var isFlashOn by remember { mutableStateOf(false) }
    val cameraActions = remember { CameraActions() }
    val openGallery = rememberGalleryLauncher { imageUri -> onNavigateToPreview(imageUri) }

    Scaffold { contentPadding ->
        CameraContent(
            modifier = Modifier.padding(contentPadding),
            isFlashOn = isFlashOn,
            onBackPressed = onBackPressed,
            cameraContent = {
                CameraPreviewContent(
                    onCaptureImage = { filePath -> onNavigateToPreview(filePath) },
                    onFlashToggled = { },
                    cameraActions = cameraActions
                )
            },
            onOpenGalleryClicked = openGallery,
            onFlashClicked = {
                cameraActions.toggleFlash()
            },
            onTakePictureClicked = {
                cameraActions.capture()
            }
        )
    }
}
