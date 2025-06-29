package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.BalanceCard
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.DateSwitcher
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.EmptyActivity
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.TransactionByMonth
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun ActivityContent(
    uiState: ActivityUiState,
    onEvent: (ActivityUiEvent) -> Unit,
    onBookmarkClicked: () -> Unit,
    onNavigateToAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.SIZE_16)
    ) {
        ActivityTitle(
            modifier = Modifier.padding(vertical = Dimensions.SIZE_24),
            onBookmarkClicked = onBookmarkClicked
        )

        DateSwitcher(
            label = stringResource(R.string.template_month_year, uiState.monthString, uiState.year),
            onPreviousClicked = { onEvent(ActivityUiEvent.OnPreviousMonth) },
            onNextClicked = { onEvent(ActivityUiEvent.OnNextMonth) }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.SIZE_48)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            if (uiState.transactions.isNotEmpty()) {
                BalanceCard(
                    modifier = Modifier.padding(vertical = Dimensions.SIZE_24),
                    balance = uiState.balance,
                    income = uiState.income,
                    expense = uiState.expense
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_24),
                    contentPadding = PaddingValues(bottom = Dimensions.SIZE_24)
                ) {
                    items(uiState.transactions) { item ->
                        TransactionByMonth(item = item)
                    }
                }
            } else {
                EmptyActivity(
                    modifier = Modifier.weight(1f),
                    onNavigateToAdd = onNavigateToAdd
                )
            }
        }
    }
}

@Composable
private fun ActivityTitle(modifier: Modifier = Modifier, onBookmarkClicked: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.label_activity),
            style = MaterialTheme.typography.displaySmall
        )

        IconButton(
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            onClick = onBookmarkClicked
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun ActivityContentPreview() {
    CofinanceTheme {
        Surface {
            ActivityContent(
                uiState = ActivityUiState(isLoading = true),
                onBookmarkClicked = { },
                onEvent = { },
                onNavigateToAdd = { }
            )
        }
    }
}