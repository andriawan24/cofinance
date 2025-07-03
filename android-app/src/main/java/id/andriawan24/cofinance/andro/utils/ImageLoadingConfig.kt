package id.andriawan24.cofinance.andro.utils

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcher
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.util.DebugLogger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ImageLoadingConfig {
    
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    // Use 20% of available memory for image cache
                    .maxSizePercent(context, 0.20)
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    // Limit disk cache to 100MB
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .components {
                // Use optimized OkHttp client for networking
                add(OkHttpNetworkFetcher.Factory(createOptimizedOkHttpClient()))
            }
            .respectCacheHeaders(false) // Better cache control
            .allowHardware(true) // Use hardware bitmaps for better performance
            .allowRgb565(true) // Use RGB565 for smaller memory footprint
            .placeholder(ColorDrawable(Color.Gray.toArgb()))
            .error(ColorDrawable(Color.Red.toArgb()))
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
    
    private fun createOptimizedOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // Connection pooling for better performance
            .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
    }
}