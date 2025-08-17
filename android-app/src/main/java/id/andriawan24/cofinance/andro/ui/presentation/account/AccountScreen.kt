package id.andriawan24.cofinance.andro.ui.presentation.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Devices.PIXEL_2_XL
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.HorizontalSpacing
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.presentation.account.models.AccountByGroup
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.TextSizes
import id.andriawan24.cofinance.domain.model.response.Account
import id.andriawan24.cofinance.utils.enums.AccountGroupType
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel = koinViewModel(),
    onNavigateToAddAccount: () -> Unit
) {
    val uiState by accountViewModel.uiState.collectAsStateWithLifecycle()

    AccountContent(
        accounts = uiState.accounts,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        balance = uiState.balance,
        onRefresh = { accountViewModel.refresh() },
        onNavigateToAddAccount = onNavigateToAddAccount
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountContent(
    modifier: Modifier = Modifier,
    accounts: List<AccountByGroup>,
    balance: Long,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(top = Dimensions.SIZE_16),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.label_account),
                style = MaterialTheme.typography.displaySmall
            )
        }

        AssetCard(
            modifier = Modifier
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(top = Dimensions.SIZE_24),
            balance = balance,
            onAddAccountClicked = onNavigateToAddAccount
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                onRefresh()
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.SIZE_12),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_24),
                horizontalAlignment = Alignment.CenterHorizontally,
                overscrollEffect = null,
                contentPadding = PaddingValues(vertical = Dimensions.SIZE_12)
            ) {
                if (isLoading) {
                    item { CircularProgressIndicator() }
                } else {
                    items(accounts) { group ->
                        AccountGroupCard(group = group)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetCard(
    modifier: Modifier = Modifier,
    balance: Long,
    onAddAccountClicked: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Image(
            modifier = Modifier
                .padding(end = Dimensions.SIZE_8)
                .align(alignment = Alignment.BottomEnd),
            painter = painterResource(R.drawable.img_account_bg),
            contentDescription = null
        )

        Column(modifier = Modifier.padding(Dimensions.SIZE_16)) {
            Text(
                "Total Assets",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeightStyle = LineHeightStyle(
                        LineHeightStyle.Alignment.Center,
                        LineHeightStyle.Trim.Both
                    )
                )
            )

            VerticalSpacing(Dimensions.SIZE_8)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Rp",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeightStyle = LineHeightStyle(
                            LineHeightStyle.Alignment.Center,
                            LineHeightStyle.Trim.Both
                        )
                    )
                )
                HorizontalSpacing(Dimensions.SIZE_2)
                Text(
                    text = NumberHelper.formatNumber(balance),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeightStyle = LineHeightStyle(
                            LineHeightStyle.Alignment.Center,
                            LineHeightStyle.Trim.Both
                        )
                    )
                )
            }

            VerticalSpacing(Dimensions.SIZE_16)

            SecondaryButton(
                verticalPadding = Dimensions.SIZE_6,
                horizontalPadding = Dimensions.SIZE_16,
                onClick = onAddAccountClicked,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.SIZE_16)
                    )

                    Text(
                        text = "Add Account",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = TextSizes.SIZE_12,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        }
    }
}

@Preview(device = PIXEL_2_XL, showBackground = true)
@Composable
private fun AccountScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AccountContent(
                accounts = listOf(
                    AccountByGroup(
                        groupLabel = AccountGroupType.CASH.displayName,
                        totalAmount = 110_000,
                        backgroundColor = Color(0xFFEEF9F8),
                        imageRes = R.drawable.ic_money,
                        accounts = listOf(
                            Account(
                                name = "BCA",
                                balance = 10_000
                            ),
                            Account(
                                name = "BSI",
                                balance = 100_000
                            )
                        )
                    ),
                    AccountByGroup(
                        groupLabel = AccountGroupType.SAVINGS.displayName,
                        totalAmount = 50_000,
                        backgroundColor = Color(0xFFFFF4FD),
                        imageRes = R.drawable.ic_saving,
                        accounts = listOf(
                            Account(
                                name = "BCA",
                                balance = 40_000
                            ),
                            Account(
                                name = "BSI",
                                balance = 10_000
                            )
                        )
                    )
                ),
                isLoading = false,
                onNavigateToAddAccount = { },
                balance = 1000,
                onRefresh = {},
                isRefreshing = false
            )
        }
    }
}