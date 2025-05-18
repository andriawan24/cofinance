package id.andriawan24.cofinance.andro.ui.presentation.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.dropShadow

@Composable
fun ExpenseByMonth(modifier: Modifier = Modifier) {
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
            .padding(Dimensions.SIZE_16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Thu, 15 May 2025",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "Rp85.000",
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
            repeat(4) {
                ExpenseItem()
            }
        }
    }
}

@Preview
@Composable
private fun ExpenseByMonthPreview() {
    CofinanceTheme {
        ExpenseByMonth(
            modifier = Modifier.fillMaxWidth()
        )
    }
}