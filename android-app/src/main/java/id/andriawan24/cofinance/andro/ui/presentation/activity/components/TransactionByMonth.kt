package id.andriawan24.cofinance.andro.ui.presentation.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.ext.FORMAT_MONTH
import id.andriawan24.cofinance.andro.utils.ext.dropShadow
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.domain.model.response.TransactionByDate

@Composable
fun TransactionByMonth(
    modifier: Modifier = Modifier,
    item: TransactionByDate
) {
    Column(
        modifier = modifier
            .dropShadow(
                shape = MaterialTheme.shapes.large,
                blur = Dimensions.SIZE_10,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                offsetY = Dimensions.SIZE_4
            )
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .padding(all = Dimensions.SIZE_16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.dateLabel.formatToString(format = FORMAT_MONTH),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = NumberHelper.formatRupiah(item.totalAmount),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Dimensions.SIZE_16),
            thickness = Dimensions.SIZE_1,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_24)) {
            item.transactions.forEach {
                TransactionItem(transaction = it)
            }
        }
    }
}

@Preview
@Composable
private fun TransactionByMonthPreview() {
    CofinanceTheme {
        TransactionByMonth(
            modifier = Modifier.fillMaxWidth(),
            item = TransactionByDate(dateLabel = Pair(2025, 0))
        )
    }
}