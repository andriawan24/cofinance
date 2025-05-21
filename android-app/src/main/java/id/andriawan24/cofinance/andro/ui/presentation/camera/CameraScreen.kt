package id.andriawan24.cofinance.andro.ui.presentation.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(appState: CofinanceAppState) {
    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var shouldShowRationalDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { selectedImage ->
            if (selectedImage != null) {
                appState.navController.navigate(Destinations.Preview(selectedImage.toString()))
            }
        }
    )

    LaunchedEffect(true) {
        if (!cameraPermission.status.isGranted) {
            if (cameraPermission.status.shouldShowRationale) {
                shouldShowRationalDialog = true
            } else cameraPermission.launchPermissionRequest()
        }
    }

    CameraContent(
        onBackPressed = { appState.navController.navigateUp() },
        cameraContent = {
            if (cameraPermission.status.isGranted) {
                CameraPreviewContent()
            }
        },
        onOpenGalleryClicked = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    )

    if (shouldShowRationalDialog) {
        RationalPermissionDialog(
            onDialogDismissed = {
                shouldShowRationalDialog = false
            }
        )
    }
}

@Composable
fun CameraContent(
    onBackPressed: () -> Unit,
    onOpenGalleryClicked: () -> Unit,
    cameraContent: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        cameraContent()

        IconButton(
            modifier = Modifier.padding(all = Dimensions.SIZE_24),
            onClick = onBackPressed,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = Dimensions.SIZE_40)
                .padding(bottom = Dimensions.SIZE_24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onOpenGalleryClicked,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_picture),
                    contentDescription = null
                )
            }

            Box(
                modifier = Modifier
                    .size(Dimensions.SIZE_66)
                    .border(
                        width = Dimensions.SIZE_4,
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    )
                    .clickable { }
                    .padding(Dimensions.SIZE_6)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    )
            )

            IconButton(
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flash_off),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun CameraPreviewContent(
    cameraViewModel: CameraViewModel = koinViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    context: Context = LocalContext.current
) {
    val surfaceRequest by cameraViewModel.surfaceRequest.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleOwner) {
        cameraViewModel.bindToCamera(context, lifecycleOwner)
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            modifier = Modifier.fillMaxSize(),
            surfaceRequest = request
        )
    }
}

@Composable
fun RationalPermissionDialog(onDialogDismissed: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDialogDismissed,
        title = {
            Text(
                text = stringResource(R.string.title_camera_permission_denied),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(R.string.description_camera_permission_denied),
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDialogDismissed()
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            ) {
                Text(text = stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDialogDismissed) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun CameraContentPreview() {
    CofinanceTheme {
        Surface {
            CameraContent(
                onBackPressed = { },
                cameraContent = { },
                onOpenGalleryClicked = { }
            )
        }
    }
}