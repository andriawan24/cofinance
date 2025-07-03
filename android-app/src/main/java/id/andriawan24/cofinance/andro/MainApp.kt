package id.andriawan24.cofinance.andro

import android.app.Application
import android.os.StrictMode
import androidx.startup.AppInitializer
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApp : Application() {
    
    // Application-level coroutine scope for background tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // Enable StrictMode in debug builds for performance monitoring
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        
        // Initialize critical components immediately
        initNapier()
        
        // Initialize non-critical components in background
        applicationScope.launch {
            initializeBackgroundComponents()
        }
    }

    private fun initNapier() {
        // Only initialize debug logging in debug builds
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
    }
    
    private suspend fun initializeBackgroundComponents() {
        // Initialize any heavy components here in background
        // Examples: Image cache warming, database pre-loading, etc.
        
        // Placeholder for future background initializations
        Napier.d("Background components initialized")
    }
    
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Handle memory pressure
        when (level) {
            TRIM_MEMORY_UI_HIDDEN,
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // Clear caches and free up memory
                Napier.d("Memory pressure detected, clearing caches")
                // TODO: Implement cache clearing logic
            }
        }
    }
}