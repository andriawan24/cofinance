package id.andriawan.cofinance

import coil3.PlatformContext

class WebCameraPermissionHandler : PermissionHandler {
    override fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit) {
        // WasmJS camera permission - simplified implementation
        onResult(true)
    }

    override fun checkPermission(context: PlatformContext): PermissionStatus {
        return PermissionStatus.NOT_DETERMINED
    }
}

actual fun createCameraPermissionHandler(): PermissionHandler {
    return WebCameraPermissionHandler()
}

actual fun PlatformContext.goToSystemSettings() {
    /* no-op */
}
