package id.andriawan.cofinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan.cofinance.auth.AndroidContextHolder
import id.andriawan.cofinance.pages.App

class MainActivity : ComponentActivity() {
    /**
     * Initializes the activity: enables edge-to-edge rendering, sets global Android context/activity
     * references used across the app (e.g., for Google Sign-In), and sets the Compose content to the
     * root `App()` composable.
     *
     * @param savedInstanceState The activity's previously saved state, if any.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set context holder for Google Sign-In
        AndroidContextHolder.applicationContext = applicationContext
        AndroidContextHolder.currentActivity = this

        setContent {
            App()
        }
    }

    /**
     * Update the global AndroidContextHolder to reference this activity when it resumes.
     *
     * Keeps AndroidContextHolder.currentActivity in sync with the currently resumed activity.
     */
    override fun onResume() {
        super.onResume()
        // Update activity reference when resumed
        AndroidContextHolder.currentActivity = this
    }

    /**
     * Clears this activity's reference from AndroidContextHolder when the activity is destroyed.
     *
     * If `AndroidContextHolder.currentActivity` still references this instance, sets it to `null` to avoid retaining
     * a reference to a destroyed Activity.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Clear activity reference when destroyed
        if (AndroidContextHolder.currentActivity == this) {
            AndroidContextHolder.currentActivity = null
        }
    }
}

/**
 * Provides an Android Studio preview of the root `App` composable.
 *
 * Invoked by the `@Preview` tooling to render the app UI in the IDE.
 */
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}