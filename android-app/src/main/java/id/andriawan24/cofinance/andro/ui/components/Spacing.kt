package id.andriawan24.cofinance.andro.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun VerticalSpacing(size: Dp = Dimensions.zero) {
    Spacer(modifier = Modifier.height(size))
}

@Composable
fun HorizontalSpacing(size: Dp = Dimensions.zero) {
    Spacer(modifier = Modifier.width(size))
}