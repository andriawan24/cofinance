package id.andriawan24.cofinance.andro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import id.andriawan24.cofinance.andro.di.viewModelModule
import id.andriawan24.cofinance.andro.ui.components.CofinanceBottomNavigation
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.navigation.MainNavigation
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.ext.conditional
import id.andriawan24.cofinance.di.dataModule
import id.andriawan24.cofinance.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinApplication

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            KoinApplication(
                application = {
                    androidLogger()
                    androidContext(this@MainActivity)
                    modules(dataModule, domainModule, viewModelModule)
                }
            ) {
                CofinanceTheme {
                    val darkTheme = isSystemInDarkTheme()
                    val appState = rememberCofinanceAppState()
                    val currentRoute by appState.navController.currentBackStackEntryAsState()
                    val routeIsCamera by remember { derivedStateOf { currentRoute?.destination?.route == Destinations.Camera.route } }

                    LaunchedEffect(darkTheme, routeIsCamera) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.auto(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                            ) {
                                darkTheme
                            },
                            navigationBarStyle = SystemBarStyle.auto(
                                lightScrim,
                                darkScrim,
                            ) {
                                darkTheme || routeIsCamera
                            },
                        )
                    }

                    Scaffold(
                        bottomBar = {
                            if (appState.currentDestination?.route in appState.bottomNavigationRoutes) {
                                CofinanceBottomNavigation(appState = appState)
                            }
                        },
                        snackbarHost = { SnackbarHost(appState.snackBarHostState) }
                    ) { contentPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .conditional(
                                    condition = appState.currentDestination?.route == Destinations.Login.route,
                                    trueModifier = {
                                        background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                )
                                            )
                                        )
                                    }
                                )
                                .padding(contentPadding),
                            color = Color.Transparent
                        ) {
                            MainNavigation(appState = appState)
                        }
                    }
                }
            }
        }
    }
}

private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)