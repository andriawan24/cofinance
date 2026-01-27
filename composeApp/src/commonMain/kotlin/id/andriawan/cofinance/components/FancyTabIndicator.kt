package id.andriawan.cofinance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions

@Composable
fun FancyTabIndicator(modifier: Modifier = Modifier, label: String) {
    Box(
        modifier = modifier
            .padding(all = Dimensions.SIZE_8)
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraLarge
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Preview
@Composable
private fun FancyTabIndicatorPreview() {
    CofinanceTheme {
        FancyTabIndicator(
            modifier = Modifier.height(Dimensions.SIZE_64)
                .width(Dimensions.SIZE_200),
            label = "Test"
        )
    }
}
