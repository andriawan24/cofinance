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
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        // Update activity reference when resumed
        AndroidContextHolder.currentActivity = this
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear activity reference when destroyed
        if (AndroidContextHolder.currentActivity == this) {
            AndroidContextHolder.currentActivity = null
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
