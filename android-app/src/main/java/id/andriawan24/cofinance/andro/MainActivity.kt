package id.andriawan24.cofinance.andro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import id.andriawan24.cofinance.andro.di.viewModelModule
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        setContent {
            KoinApplication(
                application = {
                    androidLogger()
                    androidContext(this@MainActivity)
                    modules(dataModule, domainModule, viewModelModule)
                }
            ) {
                CofinanceTheme {
                    val navController = rememberNavController()
                    MainNavigation(navController = navController)
                }
            }
        }
    }
}
