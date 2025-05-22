package id.andriawan24.cofinance.andro.ui.presentation.addnew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.ExpenseSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.models.ExpensesType
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.emptyString
import kotlinx.coroutines.launch

@Composable
fun AddNewScreen(appState: CofinanceAppState, totalPrice: Long, date: String, imageUri: String) {
    AddNewContent(
        onBackPressed = { appState.navController.navigateUp() },
        onInputPictureClicked = { appState.navController.navigate(Destinations.Camera) },
        totalPrice = totalPrice,
        date = date,
        imageUri = imageUri
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewContent(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onInputPictureClicked: () -> Unit,
    totalPrice: Long,
    date: String,
    imageUri: String
) {
    val scope = rememberCoroutineScope()
    val expenseTypePagerState = rememberPagerState { ExpensesType.entries.size }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = Dimensions.SIZE_12, bottom = Dimensions.SIZE_24)
                .padding(horizontal = Dimensions.SIZE_16)
        ) {
            IconButton(
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.title_add_activity),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        SecondaryTabRow(
            modifier = Modifier
                .padding(horizontal = Dimensions.SIZE_16)
                .clip(MaterialTheme.shapes.extraLarge),
            containerColor = MaterialTheme.colorScheme.onPrimary,
            selectedTabIndex = expenseTypePagerState.currentPage,
            indicator = {
                FancyIndicator(
                    modifier = Modifier.tabIndicatorOffset(expenseTypePagerState.currentPage),
                    label = ExpensesType.getByIndex(expenseTypePagerState.currentPage).label
                )
            },
            divider = { }
        ) {
            ExpensesType.entries.forEachIndexed { index, type ->
                Tab(
                    modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
                    selected = expenseTypePagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            expenseTypePagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = type.label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (index == expenseTypePagerState.currentPage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                )
            }
        }

        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = expenseTypePagerState
        ) {
            when (it) {
                ExpensesType.EXPENSES.index -> ExpenseSection(
                    modifier = Modifier.fillMaxSize(),
                    onInputPictureClicked = onInputPictureClicked,
                    totalPrice = totalPrice,
                    date = date,
                    imageUri = imageUri
                )

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "This is $it ${ExpensesType.getByIndex(it).label}")
                    }
                }
            }
        }
    }
}

@Composable
fun FancyIndicator(modifier: Modifier = Modifier, label: String) {
    Box(
        modifier
            .padding(all = Dimensions.SIZE_8)
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraLarge
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onPrimary
            )
        )
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
            AddNewContent(
                onBackPressed = { },
                onInputPictureClicked = {},
                totalPrice = 0,
                date = emptyString(),
                imageUri = emptyString()
            )
        }
    }
}