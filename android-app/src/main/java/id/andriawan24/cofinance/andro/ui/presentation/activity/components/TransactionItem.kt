package id.andriawan24.cofinance.andro.ui.presentation.activity.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.ColorHelper
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.LocaleHelper
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory
import id.andriawan24.cofinance.andro.utils.ext.FORMAT_HOUR_MINUTE
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.andro.utils.ext.toDate
import id.andriawan24.cofinance.domain.model.response.Account
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.utils.enums.TransactionType
import java.util.UUID

@Composable
fun TransactionItem(modifier: Modifier = Modifier, transaction: Transaction) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = when (transaction.type) {
                        TransactionType.TRANSFER -> Color(0xFFEEF6FF)
                        else -> TransactionCategory.getCategoryByName(transaction.category).color
                    },
                    shape = MaterialTheme.shapes.small
                )
                .padding(all = Dimensions.SIZE_12)
        ) {
            Image(
                painter = painterResource(
                    when (transaction.type) {
                        TransactionType.TRANSFER -> R.drawable.ic_transfer
                        else -> TransactionCategory.getCategoryByName(transaction.category).iconRes
                    }
                ),
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
        ) {
            Text(
                text = transaction.notes.ifEmpty {
                    when (transaction.type) {
                        TransactionType.TRANSFER -> stringResource(R.string.label_transfer)
                        else -> stringResource(
                            TransactionCategory.getCategoryByName(transaction.category).labelRes
                        )
                    }
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = stringResource(
                    id = R.string.template_dot,
                    transaction.date.toDate().formatToString(
                        format = FORMAT_HOUR_MINUTE,
                        locale = LocaleHelper.getCurrentLocale()
                    ),
                    transaction.account.name.plus(
                        if (transaction.receiverAccount.name.isNotEmpty()) {
                            " → ${transaction.receiverAccount.name}"
                        } else emptyString()
                    )
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Text(
            text = NumberHelper.formatRupiah(transaction.amount),
            style = MaterialTheme.typography.labelMedium.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                color = ColorHelper.getColorByExpenseType(transaction.type)
            )
        )
    }
}

@Preview
@Composable
private fun TransactionItemPreview() {
    CofinanceTheme {
        Surface {
            TransactionItem(
                transaction = Transaction(
                    amount = 100,
                    category = TransactionCategory.SUBSCRIPTION.toString(),
                    date = emptyString(),
                    fee = 100,
                    notes = emptyString(),
                    account = Account(name = "Test Account"),
                    type = TransactionType.EXPENSE,
                    id = UUID.randomUUID().toString()
                )
            )
        }
    }
}