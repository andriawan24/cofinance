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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    enabled: Boolean = true,
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
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        onClick = onClick,
        enabled = enabled
    ) {
        content()
    }
}

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shape: Shape = ButtonDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Dimensions.SIZE_24,
        vertical = Dimensions.SIZE_16
    ),
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        modifier = modifier,
        shape = shape,
        contentPadding = contentPadding,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        enabled = enabled,
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