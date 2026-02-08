package id.andriawan.cofinance

import coil3.PlatformContext

/**
 * Opens the system settings for the application.
 *
 * On this platform this operation is a no-op.
 */
actual fun PlatformContext.goToSystemSettings() {

}

/**
 * Creates a camera permission handler for desktop that treats camera access as granted.
 *
 * The returned handler immediately reports permission granted when asked and always
 * reports `PermissionStatus.GRANTED` when checked, since desktop platforms do not
 * require explicit camera permissions.
 *
 * @return A `PermissionHandler` that always grants camera permission on this platform.
 */
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