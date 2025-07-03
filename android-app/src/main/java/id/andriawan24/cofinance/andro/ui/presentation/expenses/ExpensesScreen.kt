package id.andriawan24.cofinance.andro.ui.presentation.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.components.PieChart
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.rememberExpensive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    val scope = rememberCoroutineScope()
    
    // Memoize tabs to prevent recreation on every recomposition
    val tabs = rememberExpensive { listOf("Income", "Expense") }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    
    // Memoize sample data to prevent recreation
    val pieChartData = rememberExpensive {
        mapOf(
            "Food & Dining" to 25,
            "Transportation" to 20,
            "Shopping" to 15,
            "Entertainment" to 12,
            "Bills & Utilities" to 18,
            "Healthcare" to 10
        )
    }
    
    // Derive selected tab index to prevent unnecessary recalculations
    val selectedTabIndex by remember {
        derivedStateOf { pagerState.currentPage }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Optimized Tab Row
        SecondaryTabRow(
            modifier = Modifier.padding(top = Dimensions.SIZE_24),
            selectedTabIndex = selectedTabIndex,
            contentColor = MaterialTheme.colorScheme.onBackground,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedTabIndex)
                        .padding(horizontal = Dimensions.SIZE_12),
                    color = MaterialTheme.colorScheme.primary,
                    height = Dimensions.SIZE_2
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
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

        // Replace heavy Column with LazyColumn for better performance
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimensions.SIZE_24),
                    text = "25 April - 24 May 2025",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            
            item {
                HorizontalPager(
                    modifier = Modifier.fillMaxWidth(),
                    state = pagerState
                ) { page ->
                    ExpensePageContent(
                        pageIndex = page,
                        pieChartData = pieChartData
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpensePageContent(
    pageIndex: Int,
    pieChartData: Map<String, Int>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.SIZE_24)
    ) {
        // Create different data based on page to demonstrate tab functionality
        val pageSpecificData = remember(pageIndex) {
            when (pageIndex) {
                0 -> pieChartData // Income data
                1 -> pieChartData.mapValues { it.value * 0.8 } // Expense data (slightly different)
                else -> pieChartData
            }
        }
        
        PieChart(
            data = pageSpecificData.mapValues { it.value.toInt() },
            detailChart = { data, colors ->
                // TODO: Handle data colors and show as optimized list
                // Consider using LazyColumn for large datasets
            }
        )
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