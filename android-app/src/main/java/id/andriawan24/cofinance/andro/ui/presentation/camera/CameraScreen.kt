package id.andriawan24.cofinance.andro.ui.presentation.camera

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.presentation.camera.components.CameraContent
import id.andriawan24.cofinance.andro.ui.presentation.camera.components.CameraPreviewContent
import id.andriawan24.cofinance.andro.ui.presentation.camera.components.RationalPermissionDialog
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    appState: CofinanceAppState,
    cameraViewModel: CameraViewModel = koinViewModel(),
    context: Context = LocalContext.current
) {
    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { selectedImage ->
            if (selectedImage != null) {
                appState.navController.navigate(Destinations.Preview(selectedImage.toString()))
            }
        }
    )

    cameraViewModel.uiEvent.CollectAsEffect {
        when (it) {
            is CameraUiEvent.ImageCaptured -> appState.navController.navigate(
                Destinations.Preview(it.imageUri.toString())
            )

            is CameraUiEvent.ShowError -> appState.showSnackbar(it.message)
        }
    }

    LaunchedEffect(true) {
        if (!cameraPermission.status.isGranted) {
            if (cameraPermission.status.shouldShowRationale) {
                cameraViewModel.showRationaleDialog(true)
            } else cameraPermission.launchPermissionRequest()
        }
    }

    CameraContent(
        isFlashOn = uiState.flashlightOn,
        onBackPressed = { appState.navController.navigateUp() },
        cameraContent = {
            if (cameraPermission.status.isGranted) {
                CameraPreviewContent(cameraViewModel = cameraViewModel)
            }
        },
        onOpenGalleryClicked = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onFlashClicked = {
            cameraViewModel.toggleFlashlight()
        },
        onTakePictureClicked = {
            cameraViewModel.captureImage(context)
        }
    )

    if (uiState.shouldShowRationalDialog) {
        RationalPermissionDialog(
            onDialogDismissed = {
                cameraViewModel.showRationaleDialog(false)
            }
        )
    }

    if (uiState.showLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
                securePolicy = SecureFlagPolicy.SecureOn
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(Dimensions.SIZE_24)
                    .clip(MaterialTheme.shapes.small)
            ) {
                CircularProgressIndicator()
            }
        }
    }
}