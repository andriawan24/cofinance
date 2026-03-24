package id.andriawan.cofinance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.andriawan.cofinance.pages.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sharedImageUri = handleShareIntent(intent)

        setContent {
            App(sharedImageUri = sharedImageUri)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle share intent when activity is already running
        val sharedImageUri = handleShareIntent(intent)
        if (sharedImageUri != null) {
            setContent {
                App(sharedImageUri = sharedImageUri)
            }
        }
    }

    private fun handleShareIntent(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            return imageUri?.toString()
        }
        return null
    }
}

