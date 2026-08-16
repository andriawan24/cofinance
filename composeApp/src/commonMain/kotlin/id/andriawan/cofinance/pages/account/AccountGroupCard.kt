package id.andriawan.cofinance.pages.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.domain.model.response.toDrawable
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import id.andriawan.cofinance.utils.enums.AccountGroupType
import id.andriawan.cofinance.utils.enums.AccountGroupType.Companion.getBackgroundColor
import id.andriawan.cofinance.utils.enums.AccountType
import org.jetbrains.compose.resources.painterResource

@Composable
fun AccountGroupCard(
    modifier: Modifier = Modifier,
    group: AccountByGroup,
    onAccountClicked: (Account) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SIZE_16),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.SIZE_44)
                        .background(
                            color = Color(group.backgroundColor),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(group.accountGroupType.toDrawable()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.groupLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Text(
                        text = "${group.accounts.size} account${if (group.accounts.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Text(
                    text = NumberHelper.formatRupiah(group.totalAmount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            HorizontalDivider(
                thickness = Dimensions.SIZE_1,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_10)) {
                group.accounts.forEachIndexed { index, account ->
                    AccountRow(
                        account = account,
                        backgroundColor = Color(group.backgroundColor),
                        onAccountClicked = onAccountClicked
                    )

                    if (index != group.accounts.lastIndex) {
                        HorizontalDivider(
                            thickness = Dimensions.SIZE_1,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    backgroundColor: Color,
    onAccountClicked: (Account) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClicked(account) }
            .sizeIn(minHeight = Dimensions.SIZE_48)
            .padding(vertical = Dimensions.SIZE_4),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.SIZE_10)
                .background(
                    color = backgroundColor.copy(alpha = 0.9f),
                    shape = CircleShape
                )
        )

        Text(
            modifier = Modifier.weight(1f),
            text = account.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Text(
            text = NumberHelper.formatRupiah(account.balance),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Preview
@Composable
private fun AccountGroupCardPreview() {
    CofinanceTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccountGroupCard(
                modifier = Modifier.padding(Dimensions.SIZE_24),
                group = AccountByGroup(
                    groupLabel = "Cash",
                    totalAmount = 15_750_000,
                    backgroundColor = AccountGroupType.CASH.getBackgroundColor(),
                    accountGroupType = AccountGroupType.CASH,
                    accounts = listOf(
                        Account(
                            id = "1",
                            name = "Wallet",
                            group = AccountGroupType.CASH,
                            balance = 500_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-01"
                        ),
                        Account(
                            id = "2",
                            name = "Emergency Fund",
                            group = AccountGroupType.CASH,
                            balance = 2_000_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-02"
                        ),
                        Account(
                            id = "3",
                            name = "BCA Savings",
                            group = AccountGroupType.CASH,
                            balance = 3_250_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-03"
                        ),
                        Account(
                            id = "4",
                            name = "Mandiri Checking",
                            group = AccountGroupType.CASH,
                            balance = 1_100_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-04"
                        ),
                        Account(
                            id = "5",
                            name = "Petty Cash",
                            group = AccountGroupType.CASH,
                            balance = 150_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-05"
                        ),
                        Account(
                            id = "6",
                            name = "Travel Fund",
                            group = AccountGroupType.CASH,
                            balance = 1_800_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-06"
                        ),
                        Account(
                            id = "7",
                            name = "GoPay",
                            group = AccountGroupType.CASH,
                            balance = 275_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-07"
                        ),
                        Account(
                            id = "8",
                            name = "OVO",
                            group = AccountGroupType.CASH,
                            balance = 320_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-08"
                        ),
                        Account(
                            id = "9",
                            name = "DANA",
                            group = AccountGroupType.CASH,
                            balance = 4_500_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-09"
                        ),
                        Account(
                            id = "10",
                            name = "ShopeePay",
                            group = AccountGroupType.CASH,
                            balance = 1_855_000,
                            accountType = AccountType.REGULAR_BALANCE,
                            createdAt = "2024-01-10"
                        )
                    )
                )
            )
        }
    }
}
