package id.andriawan.cofinance.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.DpOffset
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_expense
import cofinance.composeapp.generated.resources.ic_income
import cofinance.composeapp.generated.resources.img_balance_bg
import cofinance.composeapp.generated.resources.label_balance
import cofinance.composeapp.generated.resources.label_expenses
import cofinance.composeapp.generated.resources.label_income
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BalanceCard(modifier: Modifier = Modifier, balance: Long, income: Long, expense: Long) {
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
                iconRes = Res.drawable.ic_income,
                label = stringResource(Res.string.label_income),
                value = income,
                valueColor = Color(0xFF045330)
            )

            IncomeExpenseLabel(
                modifier = Modifier.weight(1f),
                iconRes = Res.drawable.ic_expense,
                label = stringResource(Res.string.label_expenses),
                value = expense,
                Color(0xFFC50102)
            )
        }
    }
}

@Composable
private fun IncomeExpenseLabel(
    modifier: Modifier = Modifier,
    iconRes: DrawableResource,
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
            painter = painterResource(Res.drawable.img_balance_bg),
            contentDescription = null
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_16)
        ) {
            Text(
                text = stringResource(Res.string.label_balance),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeightStyle = LineHeightStyle(
                        LineHeightStyle.Alignment.Center,
                        LineHeightStyle.Trim.Both
                    )
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

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

                Spacer(modifier = Modifier.width(Dimensions.SIZE_2))

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
