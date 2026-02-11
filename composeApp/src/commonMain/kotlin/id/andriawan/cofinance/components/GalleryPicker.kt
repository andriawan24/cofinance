package id.andriawan.cofinance.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberGalleryLauncher(onImageSelected: (String) -> Unit): () -> Unit
