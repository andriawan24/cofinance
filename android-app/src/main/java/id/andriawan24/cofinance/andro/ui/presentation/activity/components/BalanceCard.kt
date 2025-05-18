package id.andriawan24.cofinance.andro.ui.presentation.activity.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.HorizontalSpacing
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.ext.dropShadow

@Composable
fun BalanceCard(modifier: Modifier = Modifier, balance: Long, income: Long, expense: Long) {
    Column(
        modifier = modifier
            .dropShadow(
                shape = RoundedCornerShape(size = Dimensions.SIZE_16),
                blur = Dimensions.SIZE_10,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                offsetY = Dimensions.SIZE_4
            )
    ) {
        Balance(balance = balance)
        IncomeExpenses(income = income, expense = expense)
    }
}

@Composable
private fun IncomeExpenses(income: Long, expense: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(
                    bottomStart = Dimensions.SIZE_16,
                    bottomEnd = Dimensions.SIZE_16
                )
            )
            .padding(Dimensions.SIZE_16)
    ) {


        Row(modifier = Modifier.weight(1f)) {
            IncomeExpenseLabel(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_income,
                label = stringResource(R.string.label_income),
                value = income,
                valueColor = colorResource(R.color.green)
            )

            IncomeExpenseLabel(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_expense,
                label = stringResource(R.string.label_expenses),
                value = expense,
                colorResource(R.color.red)
            )
        }
    }
}

@Composable
private fun IncomeExpenseLabel(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    label: String,
    value: Long,
    valueColor: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_2)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = NumberHelper.formatRupiah(value),
                style = MaterialTheme.typography.labelMedium.copy(color = valueColor)
            )
        }
    }
}

@Composable
private fun Balance(modifier: Modifier = Modifier, balance: Long) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    topStart = Dimensions.SIZE_16,
                    topEnd = Dimensions.SIZE_16
                )
            )
    ) {
        Image(
            modifier = Modifier.align(Alignment.BottomEnd),
            painter = painterResource(R.drawable.img_balance_bg),
            contentDescription = null
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_16)
        ) {
            Text(
                text = stringResource(R.string.label_balance),
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
        }
    }
}

@Preview
@Composable
private fun BalanceCardPreview() {
    CofinanceTheme {
        BalanceCard(
            modifier = Modifier.fillMaxWidth(),
            balance = 6000000,
            income = 10000000,
            expense = 4000000
        )
    }
}