package id.andriawan.cofinance

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import id.andriawan.cofinance.data.lock.BiometricPromptHost
import id.andriawan.cofinance.pages.App

/**
 * The app's single activity, and the host `androidx.biometric` shows its prompt from.
 *
 * ## Why `AppCompatActivity` rather than `ComponentActivity`
 *
 * `BiometricPrompt` requires a `FragmentActivity`: it puts an invisible fragment into the host's
 * fragment manager and drives authentication from there, and `ComponentActivity` has no fragment
 * manager to put it in. With a `ComponentActivity` the prompt host stays null and every biometric
 * call reports `Unavailable`, which is the state this change ends.
 *
 * `AppCompatActivity` rather than a bare `FragmentActivity` because `minSdk` is 24. On API 28 and
 * above `BiometricPrompt` delegates to the platform prompt, but below it `androidx.biometric` draws
 * its own fingerprint dialog out of AppCompat resources — which is why the library declares a
 * runtime dependency on AppCompat. A bare `FragmentActivity` under a platform Material theme would
 * work on the device this was verified against, and fail on the older half of the install base. The
 * window theme moves with it, from `Theme.Material.Light.NoActionBar` to the AppCompat equivalent
 * that `AppCompatActivity` requires; the app's own colors come from Compose either way, so nothing
 * user-visible changes.
 *
 * ## Lifetime of the host reference
 *
 * [BiometricPromptHost.current] is set in `onCreate` and cleared in `onDestroy`, and the clear is
 * guarded so a recreated activity that has already registered itself is not unregistered by its
 * predecessor's teardown. A stale activity in that field is a leaked window and a prompt shown from
 * a dead host, so the clearing matters as much as the setting.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        BiometricPromptHost.current = this

        val sharedImageUri = handleShareIntent(intent)

        setContent {
            App(sharedImageUri = sharedImageUri)
        }
    }

    override fun onDestroy() {
        if (BiometricPromptHost.current === this) {
            BiometricPromptHost.current = null
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sharedImageUri = handleShareIntent(intent)
        if (sharedImageUri != null) {
            setContent {
                App(sharedImageUri = sharedImageUri)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun handleShareIntent(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            return imageUri?.toString()
        }

        return null
    }
}
