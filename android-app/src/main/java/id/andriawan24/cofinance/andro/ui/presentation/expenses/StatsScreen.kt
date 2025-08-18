package id.andriawan24.cofinance.andro.ui.presentation.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PieChart
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.DateSwitcher
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory
import id.andriawan24.cofinance.andro.utils.ext.dropShadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Title(
            modifier = Modifier.padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_24
            )
        )

        DateSwitcher(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = "May 2025",
            onPreviousClicked = {

            },
            onNextClicked = {

            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(top = Dimensions.SIZE_16)
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
            PieChart(
                data = mapOf(
                    TransactionCategory.ADMINISTRATION to 90,
                    TransactionCategory.SUBSCRIPTION to 100,
                    TransactionCategory.EDUCATION to 10
                ),
                detailChart = { data ->
                    Row {
                        Text("Food: 90")
                    }
                }
            )
        }
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.title_statistic),
            style = MaterialTheme.typography.displaySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            StatsScreen()
        }
    }
}