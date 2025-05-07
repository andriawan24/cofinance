package id.andriawan24.cofinance.andro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import id.andriawan24.cofinance.andro.ui.components.CofinanceBottomNavigation
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.MainNavigation
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CofinanceTheme {
                val appState = rememberCofinanceAppState()
                Scaffold(
                    bottomBar = {
                        if (appState.currentDestination?.route in appState.bottomNavigationRoutes) {
                            CofinanceBottomNavigation(appState = appState)
                        }
                    }
                ) {
                    Surface(
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(it)
                    ) {
                        MainNavigation(appState = appState)
                    }
                }
            }
        }
    }
}