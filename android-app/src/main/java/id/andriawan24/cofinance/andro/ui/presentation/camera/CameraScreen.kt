package id.andriawan24.cofinance.andro.ui.presentation.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import id.andriawan24.cofinance.andro.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var shouldShowRationalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        if (!cameraPermission.status.isGranted) {
            if (cameraPermission.status.shouldShowRationale) {
                shouldShowRationalDialog = true
            } else {
                cameraPermission.launchPermissionRequest()
            }
        }
    }

    if (cameraPermission.status.isGranted) {
        CameraPreviewContent()
    } else {
        Text("Hello World")
    }

    if (shouldShowRationalDialog) {
        RationalPermissionDialog(
            onDialogDismissed = {
                shouldShowRationalDialog = false
            }
        )
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
            surfaceRequest = request,
            implementationMode = when {
                Build.VERSION.SDK_INT > 24 -> ImplementationMode.EXTERNAL
                else -> ImplementationMode.EMBEDDED
            }
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
                text = "Camera Permission Denied",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "The camera permission is required in order to used this feature. Please grant the permission.",
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
            TextButton(
                onClick = {
                    onDialogDismissed()
                }
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}