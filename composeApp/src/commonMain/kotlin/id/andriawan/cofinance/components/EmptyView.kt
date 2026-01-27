package id.andriawan.cofinance.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_add_activity
import cofinance.composeapp.generated.resources.content_description_empty_state_image
import cofinance.composeapp.generated.resources.description_empty_activity
import cofinance.composeapp.generated.resources.img_empty_activity
import cofinance.composeapp.generated.resources.title_no_activities
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmptyView(modifier: Modifier = Modifier, onNavigateToAdd: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_32),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.img_empty_activity),
            contentDescription = stringResource(Res.string.content_description_empty_state_image)
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

        Text(
            text = stringResource(Res.string.title_no_activities),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

        Text(
            text = stringResource(Res.string.description_empty_activity),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        SecondaryButton(
            modifier = Modifier,
            contentPadding = PaddingValues(
                vertical = Dimensions.SIZE_12,
                horizontal = Dimensions.SIZE_46
            ),
            onClick = onNavigateToAdd
        ) {
            Text(
                text = stringResource(Res.string.action_add_activity),
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
            EmptyView(
                modifier = Modifier.fillMaxHeight(),
                onNavigateToAdd = { }
            )
        }
    }
}
