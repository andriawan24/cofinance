package id.andriawan.cofinance.pages.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.description_no_accounts
import cofinance.composeapp.generated.resources.label_account
import cofinance.composeapp.generated.resources.label_account_type_asset
import cofinance.composeapp.generated.resources.label_account_type_regular
import cofinance.composeapp.generated.resources.label_add_account
import cofinance.composeapp.generated.resources.title_no_accounts
import id.andriawan.cofinance.components.BaseBottomSheet
import id.andriawan.cofinance.components.EditAccountBottomSheetContent
import id.andriawan.cofinance.components.PageTitle
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.pages.account.components.AssetCard
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.enums.AccountGroupType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel = koinViewModel(),
    onNavigateToAddAccount: () -> Unit
) {
    val uiState by accountViewModel.uiState.collectAsStateWithLifecycle()

    AccountContent(
        uiState = uiState,
        onRefresh = accountViewModel::refreshAccounts,
        onNavigateToAddAccount = onNavigateToAddAccount,
        onAccountClicked = accountViewModel::onAccountClicked
    )

    if (uiState.editingAccount != null) {
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        BaseBottomSheet(
            state = editSheetState,
            onDismissRequest = { accountViewModel.onDismissEditAccount() }
        ) {
            EditAccountBottomSheetContent(
                account = uiState.editingAccount!!,
                onSaveClicked = { name, balance, group, accountType ->
                    accountViewModel.onSaveAccount(
                        uiState.editingAccount!!.id,
                        name,
                        balance,
                        group,
                        accountType
                    )
                },
                onDeleteClicked = {
                    accountViewModel.onDeleteAccount(uiState.editingAccount!!.id)
                },
                onCloseClicked = { accountViewModel.onDismissEditAccount() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onRefresh: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onAccountClicked: (Account) -> Unit = {},
) {
    val groupedAccounts = when {
        uiState.assetAccounts.isNotEmpty() || uiState.regularAccounts.isNotEmpty() -> {
            listOf(
                stringResource(Res.string.label_account_type_asset) to uiState.assetAccounts,
                stringResource(Res.string.label_account_type_regular) to uiState.regularAccounts
            ).filter { it.second.isNotEmpty() }
        }

        else -> listOf("" to uiState.accounts).filter { it.second.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_24,
            ),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        PageTitle(title = stringResource(Res.string.label_account))

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)) {
                item {
                    AssetCard(
                        balance = uiState.balance,
                        onAddAccountClicked = onNavigateToAddAccount
                    )
                }

                when {
                    uiState.isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimensions.SIZE_48),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    groupedAccounts.isEmpty() -> {
                        item {
                            EmptyAccountsState(onAddAccountClicked = onNavigateToAddAccount)
                        }
                    }

                    else -> {
                        groupedAccounts.forEach { (title, groups) ->
                            if (title.isNotBlank()) {
                                item {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                }
                            }

                            items(groups) { group ->
                                AccountGroupCard(
                                    group = group,
                                    onAccountClicked = onAccountClicked
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsState(onAddAccountClicked: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
        ) {
            Text(
                text = stringResource(Res.string.title_no_accounts),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = stringResource(Res.string.description_no_accounts),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            SecondaryButton(
                modifier = Modifier.sizeIn(minHeight = Dimensions.SIZE_44),
                onClick = onAddAccountClicked
            ) {
                Text(
                    text = stringResource(Res.string.label_add_account),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview
@Composable
private fun AccountScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AccountContent(
                uiState = UiState(
                    accounts = listOf(
                        AccountByGroup(
                            groupLabel = "Cash",
                            totalAmount = 110_000,
                            backgroundColor = 0xFFEEF9F8,
                            accountGroupType = AccountGroupType.CASH,
                            accounts = listOf(
                                Account(name = "BCA", balance = 10_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                                Account(name = "BSI", balance = 100_000),
                            )
                        )
                    ),
                    balance = 110_000
                ),
                onRefresh = {},
                onNavigateToAddAccount = {}
            )
        }
    }
}
