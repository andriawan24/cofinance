package id.andriawan.cofinance

import coil3.PlatformContext
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

actual fun PlatformContext.goToSystemSettings() {
    /* no-op */
}

class WebCameraPermissionHandler : PermissionHandler {
    override fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val stream = window.navigator.mediaDevices.getUserMedia(
                    js("{ video: true }")
                ).await()

                stream.getTracks().forEach { it.stop() }

                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    override fun checkPermission(context: PlatformContext): PermissionStatus {
        return PermissionStatus.NOT_DETERMINED
    }
}

actual fun createCameraPermissionHandler(): PermissionHandler {
    return WebCameraPermissionHandler()
}
