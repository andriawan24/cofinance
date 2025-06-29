package id.andriawan24.cofinance.andro.ui.presentation.activity.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun EmptyActivity(modifier: Modifier = Modifier, onNavigateToAdd: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_32),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.img_empty_activity),
            contentDescription = "Empty state image for activity"
        )

        VerticalSpacing(Dimensions.SIZE_8)

        Text(
            text = "No Activities Yet",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        VerticalSpacing(Dimensions.SIZE_4)

        Text(
            text = "Start tracking your expense now",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        VerticalSpacing(Dimensions.SIZE_24)

        SecondaryButton(
            modifier = Modifier,
            horizontalPadding = Dimensions.SIZE_46,
            verticalPadding = Dimensions.SIZE_12,
            onClick = onNavigateToAdd
        ) {
            Text(
                text = stringResource(R.string.action_add_activity),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Preview
@Composable
private fun EmptyActivityPreview() {
    CofinanceTheme {
        Surface {
            EmptyActivity(
                modifier = Modifier.fillMaxHeight(),
                onNavigateToAdd = { }
            )
        }
    }
}