package id.andriawan.cofinance.pages.camera

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.Uri
import coil3.compose.LocalPlatformContext
import id.andriawan.cofinance.components.CameraContent
import id.andriawan.cofinance.components.CameraPreviewContent
import id.andriawan.cofinance.createCameraPermissionHandler
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CameraScreen(
    onNavigateToPreview: (Uri) -> Unit,
    onBackPressed: () -> Unit
) {
    val permissionHandler = remember { createCameraPermissionHandler() }
    var hasPermission by remember { mutableStateOf(false) }
    val context = LocalPlatformContext.current

//    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)
//    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()
//    val context = LocalContext.current
//
//    val imagePickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.PickVisualMedia(),
//        onResult = { selectedImage ->
//            if (selectedImage != null) {
//                onNavigateToPreview(selectedImage)
//            }
//        }
//    )

//    cameraViewModel.uiEvent.CollectAsEffect {
//        when (it) {
//            is CameraUiEvent.ImageCaptured -> onNavigateToPreview(it.imageUri)
//            is CameraUiEvent.ShowError -> {
//                // TODO: Show snackbar later
//            }
//        }
//    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionHandler.askPermission(context) { isGranted ->
                hasPermission = isGranted
            }
        }
    }

    Scaffold { contentPadding ->
        CameraContent(
            modifier = Modifier.padding(contentPadding),
            isFlashOn = false,
            onBackPressed = onBackPressed,
            cameraContent = {
                if (hasPermission) {
                    CameraPreviewContent()
                }
            },
            onOpenGalleryClicked = {
                // TODO: Implement open gallery later
//                imagePickerLauncher.launch(
//                    input = PickVisualMediaRequest(
//                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
//                    )
//                )
            },
            onFlashClicked = {
                // cameraViewModel.toggleFlashlight()
            },
            onTakePictureClicked = {
                // cameraViewModel.captureImage(context)
            }
        )
    }

//    if (uiState.shouldShowRationalDialog) {
//        RationalPermissionDialog(
//            onDialogDismissed = { cameraViewModel.showRationaleDialog(false) }
//        )
//    }

//    if (uiState.showLoading) {
//        Dialog(
//            onDismissRequest = {},
//            properties = DialogProperties(
//                dismissOnClickOutside = false,
//                dismissOnBackPress = false
//            )
//        ) {
//            Box(
//                modifier = Modifier.padding(Dimensions.SIZE_24)
//                    .clip(MaterialTheme.shapes.small)
//            ) {
//                CircularProgressIndicator()
//            }
//        }
//    }
}
