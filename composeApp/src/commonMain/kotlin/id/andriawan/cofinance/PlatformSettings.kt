package id.andriawan.cofinance

import coil3.PlatformContext

enum class PermissionStatus {
    GRANTED, DENIED, NOT_DETERMINED
}

interface PermissionHandler {
    /**
 * Requests permission using the provided platform context and delivers the user's decision.
 *
 * @param onResult Callback invoked with `true` if the permission was granted, `false` otherwise.
 */
fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit)
    /**
 * Determines the current camera permission status for the given context.
 *
 * @param context PlatformContext used to query the system permission state.
 * @return `PermissionStatus.GRANTED` if permission is granted, `PermissionStatus.DENIED` if permission is denied, `PermissionStatus.NOT_DETERMINED` if the status cannot be determined.
 */
fun checkPermission(context: PlatformContext): PermissionStatus
}

/**
 * Creates a platform-specific PermissionHandler for camera access.
 *
 * @return A PermissionHandler that can request camera permission and check its current status.
 */
expect fun createCameraPermissionHandler(): PermissionHandler

/**
 * Navigates the user to this application's system settings screen.
 *
 * Platform implementations should open the appropriate OS settings page where the user can
 * view or modify app permissions and system-level configuration for the calling application.
 */
expect fun PlatformContext.goToSystemSettings()