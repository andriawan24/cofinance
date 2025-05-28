package id.andriawan24.cofinance.andro.ui.presentation.camera.components

import android.content.Context
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.andriawan24.cofinance.andro.ui.presentation.camera.CameraViewModel

@Composable
fun CameraPreviewContent(
    cameraViewModel: CameraViewModel,
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