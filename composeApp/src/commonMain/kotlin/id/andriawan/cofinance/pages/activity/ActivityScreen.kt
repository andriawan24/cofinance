package id.andriawan.cofinance.pages.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.label_activity
import cofinance.composeapp.generated.resources.template_month_year
import id.andriawan.cofinance.components.BalanceCard
import id.andriawan.cofinance.components.EmptyView
import id.andriawan.cofinance.components.TransactionByMonth
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.components.PageTitle
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.DateSwitcher
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ActivityScreen(
    onNavigateToAdd: () -> Unit,
    activityViewModel: ActivityViewModel = koinViewModel()
) {
    val uiState by activityViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        activityViewModel.getBalance()
        activityViewModel.fetchTransaction()
    }

    ActivityContent(
        uiState = uiState,
        onEvent = {
            activityViewModel.onEvent(it)
        },
        onBookmarkClicked = {
            // TODO: Handle bookmark page open
        },
        onNavigateToAdd = onNavigateToAdd
    )
}

@Composable
fun ActivityContent(
    uiState: ActivityUiState,
    onEvent: (ActivityUiEvent) -> Unit,
    onBookmarkClicked: () -> Unit,
    onNavigateToAdd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageTitle(
            modifier = Modifier.padding(Dimensions.SIZE_16, Dimensions.SIZE_24),
            title = stringResource(Res.string.label_activity),
            endContent = {
//                IconButton(
//                    colors = IconButtonDefaults.filledIconButtonColors(
//                        containerColor = MaterialTheme.colorScheme.primaryContainer,
//                        contentColor = MaterialTheme.colorScheme.primary
//                    ),
//                    onClick = onBookmarkClicked
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_bookmark),
//                        contentDescription = null
//                    )
//                }
            }
        )

        DateSwitcher(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(
                Res.string.template_month_year,
                uiState.monthString,
                uiState.year
            ),
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
                    modifier = Modifier.padding(
                        horizontal = Dimensions.SIZE_16,
                        vertical = Dimensions.SIZE_24
                    ),
                    balance = uiState.balance,
                    income = uiState.income,
                    expense = uiState.expense
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(space = Dimensions.SIZE_24),
                    contentPadding = PaddingValues(bottom = Dimensions.SIZE_16)
                ) {
                    items(items = uiState.transactions) { item ->
                        TransactionByMonth(
                            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
                            item = item
                        )
                    }
                }
            } else {
                EmptyView(
                    modifier = Modifier.weight(1f),
                    onNavigateToAdd = onNavigateToAdd
                )
            }
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
