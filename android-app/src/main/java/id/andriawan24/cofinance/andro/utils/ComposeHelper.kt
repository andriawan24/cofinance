package id.andriawan24.cofinance.andro.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun <T> Flow<T>.CollectAsEffect(block: (T) -> Unit) {
    LaunchedEffect(this) {
        onEach(block)
            .collect()
    }
}