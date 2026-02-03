package id.andriawan.cofinance

import coil3.PlatformContext

actual fun PlatformContext.goToSystemSettings() {

}

actual fun createCameraPermissionHandler(): PermissionHandler {
    return object : PermissionHandler {
        override fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit) {
            // Desktop doesn't require camera permissions
            onResult(true)
        }

        override fun checkPermission(context: PlatformContext): PermissionStatus {
            return PermissionStatus.GRANTED
        }
    }
}
