package id.andriawan.cofinance.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.LocalPlatformContext

/**
 * Hosts a camera preview UI and prepares Compose-scoped platform and lifecycle references for camera operations.
 *
 * This composable obtains the current `LifecycleOwner` and platform `Context` so child UI or camera controllers
 * can use them for lifecycle-aware camera binding and context-dependent resources.
 */
@Composable
fun CameraPreviewContent() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalPlatformContext.current


}