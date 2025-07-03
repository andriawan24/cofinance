package id.andriawan24.cofinance.andro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import id.andriawan24.cofinance.andro.di.viewModelModule
import id.andriawan24.cofinance.andro.ui.navigation.MainNavigation
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.di.dataModule
import id.andriawan24.cofinance.di.domainModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinApplication
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    
    private var keepSplashScreen = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            var isKoinInitialized by remember { mutableStateOf(false) }
            
            // Initialize Koin in background
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    initializeKoinIfNeeded()
                }
                isKoinInitialized = true
                keepSplashScreen = false
            }
            
            if (isKoinInitialized) {
                CofinanceTheme {
                    val navController = rememberNavController()
                    MainNavigation(navController = navController)
                }
            }
        }
    }
    
    private suspend fun initializeKoinIfNeeded() {
        // Check if Koin is already initialized to avoid re-initialization
        if (GlobalContext.getOrNull() == null) {
            // Initialize Koin modules in IO dispatcher for better performance
            withContext(Dispatchers.IO) {
                org.koin.core.context.startKoin {
                    androidLogger()
                    androidContext(this@MainActivity)
                    modules(dataModule, domainModule, viewModelModule)
                }
            }
        }
    }
}
