package id.andriawan.cofinance.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGalleryLauncher(onImageSelected: (String) -> Unit): () -> Unit {
    return { /* Gallery not supported on web */ }
}
