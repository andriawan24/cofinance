package id.andriawan.cofinance.components

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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import id.andriawan.cofinance.domain.model.response.TransactionByDate
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import id.andriawan.cofinance.utils.extensions.formatMonth
import id.andriawan.cofinance.utils.extensions.formatToString
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TransactionByMonth(
    modifier: Modifier = Modifier,
    item: TransactionByDate
) {
    Column(
        modifier = modifier
            .dropShadow(
                shape = RectangleShape,
                shadow = Shadow(
                    radius = Dimensions.SIZE_10,
                    spread = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    offset = DpOffset(x = Dimensions.zero, y = -Dimensions.SIZE_4)
                )
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
                text = item.dateLabel.formatToString(format = formatMonth),
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
