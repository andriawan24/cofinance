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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.enums.ExpenseCategory
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.andro.utils.ext.toDate
import id.andriawan24.cofinance.domain.model.response.Account
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.utils.enums.TransactionType
import java.util.UUID

@Composable
fun ExpenseItem(modifier: Modifier = Modifier, transaction: Transaction) {
    val category = remember { ExpenseCategory.getCategoryByName(transaction.category) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = category.color,
                    shape = MaterialTheme.shapes.small
                )
                .padding(Dimensions.SIZE_12)
        ) {
            Image(
                painter = painterResource(category.iconRes),
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
        ) {
            Text(
                text = transaction.notes.ifEmpty { category.label },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "${
                    transaction.date.toDate().formatToString("hh:mm")
                } · ${transaction.account.name}",
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
                color = colorResource(R.color.red)
            )
        )
    }
}

@Preview
@Composable
private fun ExpenseItemPreview() {
    CofinanceTheme {
        Surface {
            ExpenseItem(
                transaction = Transaction(
                    amount = 100,
                    category = ExpenseCategory.SUBSCRIPTION.toString(),
                    date = emptyString(),
                    fee = 100,
                    notes = emptyString(),
                    account = Account(),
                    type = TransactionType.EXPENSE,
                    id = UUID.randomUUID().toString()
                )
            )
        }
    }
}