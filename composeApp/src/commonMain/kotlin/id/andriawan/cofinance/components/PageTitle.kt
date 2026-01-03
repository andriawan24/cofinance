package id.andriawan24.cofinance.andro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PageTitle(
    modifier: Modifier = Modifier,
    title: String,
    endContent: (@Composable RowScope.() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.displaySmall)
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (endContent != null) {
                endContent()
            }
        }
    }
}