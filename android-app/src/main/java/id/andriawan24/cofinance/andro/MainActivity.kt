package id.andriawan24.cofinance.andro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import id.andriawan24.cofinance.andro.di.viewModelModule
import id.andriawan24.cofinance.andro.ui.components.CofinanceBottomNavigation
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.MainNavigation
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.di.dataModule
import id.andriawan24.cofinance.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinApplication

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            KoinApplication(
                application = {
                    androidLogger()
                    androidContext(this@MainActivity)
                    modules(dataModule, domainModule, viewModelModule)
                }
            ) {
                CofinanceTheme {
                    val appState = rememberCofinanceAppState()

                    Scaffold(
                        bottomBar = {
                            if (appState.currentDestination?.route in appState.bottomNavigationRoutes) {
                                CofinanceBottomNavigation(appState = appState)
                            }
                        },
                        snackbarHost = { SnackbarHost(appState.snackBarHostState) }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                        ) {
                            MainNavigation(appState = appState)
                        }
                    }
                }
            }
        }
    }
}