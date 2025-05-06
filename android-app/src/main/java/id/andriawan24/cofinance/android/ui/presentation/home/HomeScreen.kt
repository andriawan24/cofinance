package id.andriawan24.cofinance.android.ui.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import id.andriawan24.cofinance.android.R
import id.andriawan24.cofinance.android.ui.models.CofinanceAppState
import id.andriawan24.cofinance.android.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.android.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.Dimensions
import id.andriawan24.cofinance.android.utils.TextSizes
import id.andriawan24.cofinance.android.utils.TimeHelper
import id.andriawan24.cofinance.android.utils.TimeHelper.getGreeting
import id.andriawan24.cofinance.android.utils.ext.dropShadow

@Composable
fun HomeScreen(appState: CofinanceAppState, onSeeAllTransactionClicked: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(all = Dimensions.SIZE_24)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                modifier = Modifier
                    .size(Dimensions.SIZE_36)
                    .clip(CircleShape),
                model = ImageRequest.Builder(context)
                    .data(appState.user?.photoUrl)
                    .placeholder(R.drawable.img_placeholder_profile)
                    .error(R.drawable.img_placeholder_profile)
                    .crossfade(true)
                    .build(),
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(Dimensions.SIZE_12))

            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_2)) {
                Text(
                    text = context.getGreeting(),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = appState.user?.displayName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dropShadow(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFF1E1E1E).copy(0.10f),
                    blur = Dimensions.SIZE_4,
                    offsetY = Dimensions.SIZE_2,
                    spread = Dimensions.zero
                )
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .clip(shape = MaterialTheme.shapes.medium)
                .padding(all = Dimensions.SIZE_20)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.title_total_balance_this_month),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_2))

                Text(
                    text = "IDR 560.000.000",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = TextSizes.SIZE_32),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_18))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                        ) {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            )

                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_up),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }

                        Text(
                            text = "IDR 11.500.000",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                        ) {
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            )

                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_down),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }

                        Text(
                            text = "IDR 300.002",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today's Transactions",
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(
                onClick = onSeeAllTransactionClicked
            ) {
                Text("See all")
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
        ) {
            repeat(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(horizontal = Dimensions.SIZE_12, vertical = Dimensions.SIZE_8)
                ) {
                    Column {
                        Text(
                            text = "Beli nasi kuning",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Blu by BCA",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeScreen(
                appState = rememberCofinanceAppState(),
                onSeeAllTransactionClicked = { }
            )
        }
    }
}