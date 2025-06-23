package id.andriawan24.cofinance.andro.ui.presentation.addnew

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.presentation.addnew.models.ExpensesType
import id.andriawan24.cofinance.andro.ui.presentation.addnew.sections.ExpenseSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewViewModel
import id.andriawan24.cofinance.andro.ui.presentation.common.FancyTabIndicator
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddTransactionScreen(
    transactionId: String?,
    onBackPressed: () -> Unit,
    onSuccessSave: () -> Unit,
    onInputPictureClicked: () -> Unit,
) {
    val addNewViewModel: AddNewViewModel = koinViewModel()
    val uiState by addNewViewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        if (transactionId != null) {
            addNewViewModel.getDraftedTransaction(transactionId)
        }
    }

    addNewViewModel.onSuccessSaved.CollectAsEffect { onSuccessSave.invoke() }

    addNewViewModel.showMessage.CollectAsEffect {
        scope.launch {
            snackState.showSnackbar(it)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { contentPadding ->
        AddNewContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            onEvent = {
                when (it) {
                    AddNewUiEvent.OnBackPressed -> onBackPressed.invoke()
                    AddNewUiEvent.OnPictureClicked -> onInputPictureClicked.invoke()
                    else -> addNewViewModel.onEvent(it)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewContent(
    modifier: Modifier = Modifier,
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    val expenseTypePagerState = rememberPagerState { ExpensesType.entries.size }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.SIZE_12, bottom = Dimensions.SIZE_24)
                .padding(horizontal = Dimensions.SIZE_16)
        ) {
            IconButton(
                onClick = { onEvent.invoke(AddNewUiEvent.OnBackPressed) },
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
                FancyTabIndicator(
                    modifier = Modifier.tabIndicatorOffset(expenseTypePagerState.currentPage),
                    label = stringResource(ExpensesType.getByIndex(expenseTypePagerState.currentPage).labelResId)
                )
            },
            divider = {
                // Put nothing on divider
            }
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
                            text = stringResource(id = type.labelResId),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (index == expenseTypePagerState.currentPage) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        )
                    }
                )
            }
        }

        HorizontalPager(modifier = Modifier.weight(1f), state = expenseTypePagerState) {
            when (it) {
                ExpensesType.EXPENSES.index -> ExpenseSection(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}
