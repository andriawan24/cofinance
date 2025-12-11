package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewDialogEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewDialogState
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.presentation.common.BaseBottomSheet
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory
import id.andriawan24.cofinance.andro.utils.ext.hideAndDismiss
import id.andriawan24.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBottomSheetDialog(
    dialogState: AddNewDialogState,
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    if (!dialogState.showCategoryBottomSheet) return

    val scope = rememberCoroutineScope()
    val categoryBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedCategory by remember {
        derivedStateOf {
            if (uiState.transactionType == TransactionType.INCOME) {
                uiState.incomeCategory
            } else {
                uiState.expenseCategory
            }
        }
    }

    BaseBottomSheet(
        state = categoryBottomSheetState,
        onDismissRequest = { onDialogEvent(AddNewDialogEvent.ToggleCategoryDialog(false)) }
    ) {
        CategoryBottomSheetContent(
            selectedCategory = selectedCategory,
            type = uiState.transactionType,
            onCategorySaved = { category ->
                onEvent(AddNewUiEvent.SetCategory(category))
                scope.launch {
                    categoryBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleCategoryDialog(false))
                    }
                }
            },
            onCloseCategoryClicked = {
                scope.launch {
                    categoryBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleCategoryDialog(false))
                    }
                }
            }
        )
    }
}

@Composable
fun CategoryBottomSheetContent(
    type: TransactionType,
    selectedCategory: TransactionCategory?,
    onCategorySaved: (TransactionCategory) -> Unit,
    onCloseCategoryClicked: () -> Unit
) {
    var currentSelectedCategory by remember { mutableStateOf(selectedCategory) }
    val categories = remember {
        when (type) {
            TransactionType.EXPENSE,
            TransactionType.TRANSFER,
            TransactionType.DRAFT -> TransactionCategory.getExpenseCategories()

            TransactionType.INCOME -> TransactionCategory.getIncomeCategories()
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = stringResource(R.string.label_category),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Image(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable { onCloseCategoryClicked() },
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = Dimensions.SIZE_24)
        ) {
            itemsIndexed(categories) { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Dimensions.SIZE_16, end = Dimensions.SIZE_4)
                        .padding(vertical = Dimensions.SIZE_14),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
                ) {
                    Box(
                        modifier = Modifier
                            .background(color = category.color, shape = MaterialTheme.shapes.small)
                            .padding(all = Dimensions.SIZE_8)
                    ) {
                        Icon(
                            painter = painterResource(category.iconRes),
                            contentDescription = stringResource(category.labelRes),
                            tint = Color.Unspecified
                        )
                    }

                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(category.labelRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    RadioButton(
                        selected = currentSelectedCategory == category,
                        colors = RadioButtonDefaults.colors(
                            unselectedColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            currentSelectedCategory = category
                        }
                    )
                }

                if (index != TransactionCategory.entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
                        thickness = Dimensions.SIZE_1,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                }
            }
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16, vertical = Dimensions.SIZE_24),
            enabled = currentSelectedCategory != null,
            onClick = {
                currentSelectedCategory?.let { currentSelectedCategory ->
                    onCategorySaved(currentSelectedCategory)
                }
            }
        ) {
            Text(
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun CategoryBottomSheetContentPreview() {
    CofinanceTheme {
        Surface {
            CategoryBottomSheetContent(
                type = TransactionType.INCOME,
                selectedCategory = TransactionCategory.FOOD,
                onCategorySaved = { },
                onCloseCategoryClicked = { }
            )
        }
    }
}