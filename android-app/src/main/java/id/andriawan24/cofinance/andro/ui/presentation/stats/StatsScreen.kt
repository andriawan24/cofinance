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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PieChart
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.DateSwitcher
import id.andriawan24.cofinance.andro.ui.presentation.activity.components.EmptyActivity
import id.andriawan24.cofinance.andro.ui.presentation.stats.models.StatItem
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.ext.dropShadow
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onNavigateToAdd: () -> Unit) {
    val statsViewModel: StatsViewModel = koinViewModel()
    val uiState by statsViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Title(
            modifier = Modifier.padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_24
            )
        )

        DateSwitcher(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(
                R.string.template_month_year,
                uiState.monthString,
                uiState.year
            ),
            onPreviousClicked = { statsViewModel.onEvent(event = StatsUiEvent.OnPreviousMonth) },
            onNextClicked = { statsViewModel.onEvent(event = StatsUiEvent.OnNextMonth) }
        )

        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SIZE_48)
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                if (uiState.stats.isNotEmpty()) {
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
                            data = uiState.stats,
                            totalExpenses = uiState.totalExpenses,
                            detailChart = { data ->
                                DetailPieChart(data = data)
                            }
                        )
                    }
                } else {
                    EmptyActivity(
                        modifier = Modifier.weight(1f),
                        onNavigateToAdd = onNavigateToAdd
                    )
                }
            }
        }
    }
}

@Composable
fun DetailPieChart(data: List<StatItem>) {
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
                            .background(item.category.iconColor)
                    )

                    Column {
                        Text(
                            text = stringResource(item.category.labelRes),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Text(
                            text = stringResource(
                                R.string.label_percentage,
                                item.percentage.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = NumberHelper.formatRupiah(item.amount),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
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
            StatsScreen(
                onNavigateToAdd = {

                }
            )
        }
    }
}