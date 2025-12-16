package id.andriawan24.cofinance.utils

sealed class ResultState<out T> {
    data object Loading : ResultState<Nothing>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val exception: Exception) : ResultState<Nothing>()
}