package id.andriawan.cofinance

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import coil3.PlatformContext
import java.util.UUID


/**
 * Opens this application's system settings page so the user can view or modify app-specific settings.
 *
 * This navigates to the app details screen in system Settings for the current package.
 */
actual fun PlatformContext.goToSystemSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", this.packageName, null)
    )

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    startActivity(intent)
}

/**
 * Unwraps a Context by traversing ContextWrapper delegates to locate the underlying Activity.
 *
 * @return The Activity that backs this Context, or `null` if no Activity is found.
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

class AndroidCameraPermissionHandler : PermissionHandler {
    /**
     * Requests the CAMERA runtime permission and delivers the result to the provided callback.
     *
     * If the permission is already granted the callback is invoked with `true`. If no Activity is available to perform the request the callback is invoked with `false`. Otherwise the system permission prompt is shown and the callback is invoked with `true` when granted, `false` otherwise.
     *
     * @param context The PlatformContext used to check permission state and to locate an Activity for launching the permission request.
     * @param onResult Callback invoked with `true` when the CAMERA permission is granted, `false` otherwise.
     */
    override fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }

        val activity = context.findActivity() as? ComponentActivity
        if (activity == null) {
            onResult(false)
            return
        }

        val key = "camera_permission_${UUID.randomUUID()}"
        val launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onResult(isGranted)
        }
        launcher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Checks whether the app currently holds the CAMERA permission.
     *
     * @param context The platform context used to evaluate the permission state.
     * @return `PermissionStatus.GRANTED` if the CAMERA permission is granted for the app, `PermissionStatus.DENIED` otherwise.
     */
    override fun checkPermission(context: PlatformContext): PermissionStatus {
        val isGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        return if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
}

/**
 * Creates a PermissionHandler that manages camera permission on Android.
 *
 * @return A PermissionHandler that requests and checks the `CAMERA` permission using Android APIs.
 */
actual fun createCameraPermissionHandler(): PermissionHandler {
    return AndroidCameraPermissionHandler()
}