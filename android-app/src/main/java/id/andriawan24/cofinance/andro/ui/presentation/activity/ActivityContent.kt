package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun ActivityContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.SIZE_16)
    ) {
        Row {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.displaySmall
            )
        }
    }
}

@Preview
@Composable
private fun ActivityContentPreview() {
    CofinanceTheme {
        Surface {
            ActivityContent()
        }
    }
}