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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.ic_close
import cofinance.composeapp.generated.resources.label_category
import id.andriawan.cofinance.components.BaseBottomSheet
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.pages.addnew.AddNewDialogEvent
import id.andriawan.cofinance.pages.addnew.AddNewDialogState
import id.andriawan.cofinance.pages.addnew.AddNewUiEvent
import id.andriawan.cofinance.pages.addnew.AddNewUiState
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.enums.TransactionCategory
import id.andriawan.cofinance.utils.enums.TransactionType
import id.andriawan.cofinance.utils.extensions.hideAndDismiss
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
                text = stringResource(Res.string.label_category),
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
                painter = painterResource(Res.drawable.ic_close),
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
                            painter = painterResource(category.icon),
                            contentDescription = stringResource(category.label),
                            tint = Color.Unspecified
                        )
                    }

                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(category.label),
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
                text = stringResource(Res.string.action_save),
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

