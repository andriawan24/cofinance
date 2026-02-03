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


actual fun PlatformContext.goToSystemSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", this.packageName, null)
    )

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    startActivity(intent)
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

class AndroidCameraPermissionHandler : PermissionHandler {
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

    override fun checkPermission(context: PlatformContext): PermissionStatus {
        val isGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        return if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
}

actual fun createCameraPermissionHandler(): PermissionHandler {
    return AndroidCameraPermissionHandler()
}
