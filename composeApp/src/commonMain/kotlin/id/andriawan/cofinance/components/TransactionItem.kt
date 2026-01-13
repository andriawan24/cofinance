package id.andriawan.cofinance.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_transfer
import cofinance.composeapp.generated.resources.label_transfer
import cofinance.composeapp.generated.resources.template_dot
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.ColorHelper
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import id.andriawan.cofinance.utils.emptyString
import id.andriawan.cofinance.utils.enums.TransactionCategory
import id.andriawan.cofinance.utils.enums.TransactionType
import id.andriawan.cofinance.utils.extensions.formatToString
import id.andriawan.cofinance.utils.extensions.toDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
                ).padding(all = Dimensions.SIZE_12)
        ) {
            Image(
                painter = painterResource(
                    when (transaction.type) {
                        TransactionType.TRANSFER -> Res.drawable.ic_transfer
                        else -> TransactionCategory.getCategoryByName(transaction.category).icon
                    }
                ), contentDescription = null
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
        ) {
            Text(
                text = transaction.notes.ifEmpty {
                    when (transaction.type) {
                        TransactionType.TRANSFER -> stringResource(Res.string.label_transfer)
                        else -> stringResource(TransactionCategory.getCategoryByName(transaction.category).label)
                    }
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = stringResource(
                    Res.string.template_dot,
                    transaction.date.toDate().formatToString(),
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

@OptIn(ExperimentalUuidApi::class)
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
                    id = Uuid.generateV4().toString()
                )
            )
        }
    }
}
