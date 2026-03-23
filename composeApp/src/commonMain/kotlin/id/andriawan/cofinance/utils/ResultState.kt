package id.andriawan.cofinance.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

sealed class ResultState<out T> {
    data object Loading : ResultState<Nothing>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val exception: Exception) : ResultState<Nothing>()
}

suspend fun <T> Flow<ResultState<T>>.collectResult(
    onLoading: () -> Unit = {},
    onSuccess: suspend (T) -> Unit = {},
    onError: suspend (Exception) -> Unit = {}
) {
    collectLatest { result ->
        when (result) {
            ResultState.Loading -> onLoading()
            is ResultState.Success -> onSuccess(result.data)
            is ResultState.Error -> onError(result.exception)
        }
    }
}
