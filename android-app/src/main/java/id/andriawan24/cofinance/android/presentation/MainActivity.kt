package id.andriawan24.cofinance.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import id.andriawan24.cofinance.android.components.CofinanceBottomNavigation
import id.andriawan24.cofinance.android.models.rememberCofinanceAppState
import id.andriawan24.cofinance.android.navigation.MainNavigation
import id.andriawan24.cofinance.android.theme.CofinanceTheme

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
                            .padding(it),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation(appState = appState)
                    }
                }
            }
        }
    }
}