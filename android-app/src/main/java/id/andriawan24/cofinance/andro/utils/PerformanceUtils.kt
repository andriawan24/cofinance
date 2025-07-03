package id.andriawan24.cofinance.andro.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

object PerformanceUtils {
    
    /**
     * Optimized LaunchedEffect that respects lifecycle and uses proper keys
     */
    @Composable
    fun <T> CollectAsEffect(
        flow: Flow<T>,
        key: Any? = null,
        collector: suspend (T) -> Unit
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        
        LaunchedEffect(flow, key, lifecycleOwner.lifecycle) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collector)
            }
        }
    }
    
    /**
     * Lifecycle-aware effect that only runs when the component is in the foreground
     */
    @Composable
    fun LifecycleAwareEffect(
        key: Any? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        
        LaunchedEffect(key, lifecycleOwner.lifecycle) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                block()
            }
        }
    }
    
    /**
     * Memoized calculation helper to prevent unnecessary recompositions
     */
    @Composable
    fun <T> rememberCalculation(
        vararg keys: Any?,
        calculation: () -> T
    ): T {
        return remember(*keys) {
            calculation()
        }
    }
}

/**
 * Extension function for lazy state initialization
 */
@Composable
inline fun <T> lazyState(
    vararg keys: Any?,
    crossinline initializer: () -> T
): T {
    return remember(*keys) { initializer() }
}

/**
 * Optimized remember for expensive objects
 */
@Composable
inline fun <T> rememberExpensive(
    vararg keys: Any?,
    crossinline calculation: () -> T
): T {
    return remember(*keys) {
        calculation()
    }
}