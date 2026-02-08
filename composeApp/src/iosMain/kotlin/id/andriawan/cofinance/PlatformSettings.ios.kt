package id.andriawan.cofinance

import coil3.PlatformContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * Opens the app's page in the system Settings app on iOS.
 *
 * Does nothing if the Settings URL is unavailable or cannot be opened. */
actual fun PlatformContext.goToSystemSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
    }
}

class IOSCameraPermissionHandler : PermissionHandler {
    /**
     * Requests camera (video) access and delivers whether access was granted to the provided callback.
     *
     * @param context Platform context; not used on iOS but provided for API consistency.
     * @param onResult Invoked with `true` if camera access was granted, `false` otherwise.
     */
    override fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit) {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            onResult(granted)
        }
    }

    /**
     * Determines the app's current camera permission status.
     *
     * @return `PermissionStatus.GRANTED` if camera access is authorized, `PermissionStatus.NOT_DETERMINED` if the user has not been asked yet, `PermissionStatus.DENIED` for denied or any other unhandled authorization status.
     */
    override fun checkPermission(context: PlatformContext): PermissionStatus {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return when (status) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
            AVAuthorizationStatusDenied -> PermissionStatus.DENIED
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
            else -> PermissionStatus.DENIED
        }
    }
}

/**
 * Create a camera permission handler for the current platform (iOS).
 *
 * @return A PermissionHandler that requests and checks camera (video) permission on iOS.
 */
actual fun createCameraPermissionHandler(): PermissionHandler {
    return IOSCameraPermissionHandler()
}