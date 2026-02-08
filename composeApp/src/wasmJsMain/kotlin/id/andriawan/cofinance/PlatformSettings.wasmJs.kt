package id.andriawan.cofinance

import coil3.PlatformContext

class WebCameraPermissionHandler : PermissionHandler {
    /**
     * Requests camera permission in the Web/Wasm environment and invokes the provided callback with the outcome.
     *
     * This simplified implementation for Web/Wasm immediately reports permission granted.
     *
     * @param context The platform context.
     * @param onResult Called with `true` if permission is granted, `false` otherwise.
     */
    override fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit) {
        // WasmJS camera permission - simplified implementation
        onResult(true)
    }

    /**
     * Reports the current camera permission status on the web platform.
     *
     * @return `PermissionStatus.NOT_DETERMINED` indicating the permission state is not determinable on this platform.
     */
    override fun checkPermission(context: PlatformContext): PermissionStatus {
        return PermissionStatus.NOT_DETERMINED
    }
}

/**
 * Create a camera permission handler for the web/WASM platform.
 *
 * Provides a PermissionHandler implementation suitable for web (WASM/JS) usage.
 * The handler immediately grants permission callbacks and reports `PermissionStatus.NOT_DETERMINED`
 * for permission checks to match browser/web constraints.
 *
 * @return A `PermissionHandler` that implements camera permission behavior for the web platform.
 */
actual fun createCameraPermissionHandler(): PermissionHandler {
    return WebCameraPermissionHandler()
}

/**
 * Open the system settings for the app; this implementation is a no-op on this platform.
 */
actual fun PlatformContext.goToSystemSettings() {
    /* no-op */
}