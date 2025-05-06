package id.andriawan24.cofinance.android.presentation.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.android.components.PieChart
import id.andriawan24.cofinance.android.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.Dimensions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    val scope = rememberCoroutineScope()
    val tabs = remember { listOf("Income", "Expense") }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            modifier = Modifier.padding(top = Dimensions.SIZE_24),
            selectedTabIndex = pagerState.currentPage,
            contentColor = MaterialTheme.colorScheme.onBackground,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(pagerState.currentPage)
                        .padding(horizontal = Dimensions.SIZE_12),
                    color = MaterialTheme.colorScheme.primary,
                    height = Dimensions.SIZE_2
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = Dimensions.SIZE_12),
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.SIZE_24),
                text = "25 April - 24 May 2025",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimensions.SIZE_24)
                ) {
                    PieChart(
                        data = mapOf(
                            "Tests" to 20,
                            "Testss" to 20,
                            "Testsss" to 20,
                            "Testsqsq" to 20,
                            "Testsqssqsq" to 20,
                            "Test2" to 20
                        ),
                        detailChart = { data, colors ->
                            // TODO: Handle data colors and show as list
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpensesScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ExpensesScreen()
        }
    }
}