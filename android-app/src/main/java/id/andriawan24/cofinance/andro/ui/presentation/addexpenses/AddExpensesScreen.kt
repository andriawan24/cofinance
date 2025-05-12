package id.andriawan24.cofinance.andro.ui.presentation.addexpenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

private enum class ExpensesType(val index: Int, val label: String) {
    INCOME(0, "Income"),
    EXPENSES(1, "Expense"),
    TRANSFER(2, "Transfer");

    companion object {
        fun getByIndex(index: Int): ExpensesType {
            return ExpensesType.entries.firstOrNull { it.index == index } ?: EXPENSES
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpensesScreen() {
    val focusManager = LocalFocusManager.current
    var selectedSection by remember { mutableStateOf(ExpensesType.EXPENSES) }
    val sectionPagerState = rememberPagerState(initialPage = selectedSection.index) {
        ExpensesType.entries.size
    }

    LaunchedEffect(selectedSection) {
        sectionPagerState.animateScrollToPage(selectedSection.index)
    }

    LaunchedEffect(sectionPagerState.currentPage, sectionPagerState.isScrollInProgress) {
        if (!sectionPagerState.isScrollInProgress) {
            selectedSection = ExpensesType.getByIndex(sectionPagerState.currentPage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(top = Dimensions.SIZE_24)
                .padding(horizontal = Dimensions.SIZE_24)
        ) {
            FilledIconButton(
                onClick = {

                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = "Add Expense",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        PrimaryTabRow(
            selectedTabIndex = sectionPagerState.currentPage,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            ExpensesType.entries.forEach {
                Tab(
                    selected = selectedSection.index == it.index,
                    onClick = {
                        selectedSection = it
                    },
                    text = {
                        Text(it.label)
                    }
                )
            }
        }

        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = sectionPagerState
        ) {
            when (it) {
                ExpensesType.INCOME.index -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IncomeSection()
                }

                else -> {
                    LaunchedEffect(true) {
                        focusManager.clearFocus()
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Page ${selectedSection.label}")
                    }
                }
            }

        }
    }
}

@Preview(device = Devices.PIXEL_4_XL, showBackground = true)
@Composable
private fun AddExpensesScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AddExpensesScreen()
        }
    }
}