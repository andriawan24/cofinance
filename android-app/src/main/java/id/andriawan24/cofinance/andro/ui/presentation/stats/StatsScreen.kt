package id.andriawan24.cofinance.andro.ui.presentation.stats

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PieChart
import id.andriawan24.cofinance.andro.ui.components.PieLabelType
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.DateSwitcher
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
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
            onPreviousClicked = { },
            onNextClicked = { }
        )

        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.SIZE_16)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.SIZE_16)
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
                        TransactionCategory.ADMINISTRATION to 120000,
                        TransactionCategory.SUBSCRIPTION to 100000,
                        TransactionCategory.EDUCATION to 20000,
                        TransactionCategory.SALARY to 20000,
                        TransactionCategory.INTERNET to 20000,
                        TransactionCategory.DEBT to 20000,
                        TransactionCategory.HEALTH to 20000,
                        TransactionCategory.TRANSPORT to 20000,
                    ),
                    detailChart = { data ->
                        DetailPieChart(data = data)
                    }
                )
            }
        }
    }
}

@Composable
fun DetailPieChart(data: List<PieLabelType>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimensions.SIZE_48)
    ) {
        data.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.Top) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimensions.SIZE_16)
                            .clip(RoundedCornerShape(Dimensions.SIZE_2))
                            .background(item.first.iconColor)
                    )

                    Text(
                        text = stringResource(item.first.labelRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_2)
                ) {
                    Text(
                        text = NumberHelper.formatRupiah(item.second),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Text(
                        text = "${item.third}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (index != data.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Dimensions.SIZE_16),
                    thickness = Dimensions.SIZE_1,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                )
            }
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