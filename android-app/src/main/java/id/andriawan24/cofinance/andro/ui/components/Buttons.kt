package id.andriawan24.cofinance.andro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    horizontalPadding: Dp = Dimensions.SIZE_24,
    verticalPadding: Dp = Dimensions.SIZE_16,
    content: @Composable () -> Unit
) {
    Button(
        modifier = modifier,
        contentPadding = PaddingValues(
            vertical = verticalPadding,
            horizontal = horizontalPadding
        ),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        onClick = onClick
    ) {
        content()
    }
}

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    horizontalPadding: Dp = Dimensions.SIZE_24,
    verticalPadding: Dp = Dimensions.SIZE_16,
    content: @Composable () -> Unit
) {
    Button(
        modifier = modifier,
        contentPadding = PaddingValues(
            vertical = verticalPadding,
            horizontal = horizontalPadding
        ),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        onClick = onClick
    ) {
        content()
    }
}

@Preview
@Composable
private fun ButtonsPreview() {
    CofinanceTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_24)
            ) {
                PrimaryButton(onClick = { }) {
                    Text("Primary")
                }

                SecondaryButton(onClick = { }) {
                    Text("Secondary")
                }
            }
        }
    }
}