package id.andriawan.cofinance.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.LocalPlatformContext

@Composable
fun CameraPreviewContent() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalPlatformContext.current


}
