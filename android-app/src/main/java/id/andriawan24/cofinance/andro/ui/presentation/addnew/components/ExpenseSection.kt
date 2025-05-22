package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.presentation.addnew.ExpenseViewModel
import id.andriawan24.cofinance.andro.ui.presentation.addnew.ExpensesUiEvent
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.ext.conditional
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSection(
    modifier: Modifier = Modifier,
    totalPrice: Long,
    date: String,
    imageUri: String,
    onInputPictureClicked: () -> Unit,
    expenseViewModel: ExpenseViewModel = koinViewModel()
) {
    val focusManager = LocalFocusManager.current
    val uiState by expenseViewModel.uiState.collectAsStateWithLifecycle()

    var showCategoryBottomSheet by remember { mutableStateOf(false) }
    val categoryBottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(true) {
        expenseViewModel.init(totalPrice = totalPrice, date = date, imageUri = imageUri)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        if (uiState.imageUri != null) {
            VerticalSpacing(Dimensions.SIZE_24)
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uiState.imageUri)
                    .crossfade(200)
                    .build(),
                contentDescription = null
            )
        } else {
            UploadPhotoCardButton(onInputPictureClicked = onInputPictureClicked)
        }

        InputAmount(
            inputAmount = uiState.amount,
            includeFee = uiState.includeFee,
            onIncludeFeeClicked = {
                focusManager.clearFocus()
                expenseViewModel.onEvent(ExpensesUiEvent.SetIncludeFee(true))
            },
            onInputAmountChanged = { amount ->
                expenseViewModel.onEvent(ExpensesUiEvent.SetAmount(amount))
            }
        )

        if (uiState.includeFee) {
            InputFee(
                fee = uiState.fee,
                onInputFeeChanged = { fee ->
                    expenseViewModel.onEvent(ExpensesUiEvent.SetFee(fee))
                },
                onCloseFeeClicked = {
                    focusManager.clearFocus()
                    expenseViewModel.onEvent(ExpensesUiEvent.SetIncludeFee(false))
                }
            )
        }

        AddNewSection(
            label = stringResource(R.string.label_account),
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_account),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        AddNewSection(
            label = stringResource(R.string.label_category),
            value = uiState.category,
            onSectionClicked = {
                showCategoryBottomSheet = true
            },
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        AddNewSection(
            label = stringResource(R.string.label_dates),
            value = uiState.dateTime.formatToString(),
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        InputNote(
            note = uiState.notes,
            onNoteChanged = { }
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = Dimensions.SIZE_16),
            enabled = false,
            onClick = { },
        ) {
            Text(
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    if (showCategoryBottomSheet) {
        ModalBottomSheet(
            sheetState = categoryBottomSheetState,
            onDismissRequest = {
                showCategoryBottomSheet = false
            },
        ) {
            CategoryBottomSheet()
        }
    }
}

@Composable
fun InputNote(modifier: Modifier = Modifier, note: String, onNoteChanged: (String) -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_16)
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .padding(all = Dimensions.SIZE_16),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_notes),
            contentDescription = null
        )

        Row(modifier = Modifier.weight(1f)) {
            BasicTextField(
                modifier = Modifier.weight(1f),
                value = note,
                onValueChange = onNoteChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (note.isBlank()) {
                        Text(
                            text = stringResource(R.string.label_notes),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun AddNewSection(
    label: String,
    value: String = "",
    onSectionClicked: (() -> Unit)? = null,
    startIcon: @Composable () -> Unit,
    endIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_16)
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .conditional(
                condition = onSectionClicked != null,
                trueModifier = {
                    clip(MaterialTheme.shapes.large)
                        .clickable(true) {
                            onSectionClicked?.invoke()
                        }
                }
            )
            .padding(all = Dimensions.SIZE_16),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        startIcon()
        Text(
            modifier = Modifier.weight(1f),
            text = value.ifBlank { label },
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
        )
        endIcon?.invoke()
    }
}

@Preview
@Composable
private fun ExpenseSectionPreview() {
    CofinanceTheme {
        Surface {
            ExpenseSection(
                totalPrice = 0,
                date = emptyString(),
                imageUri = emptyString(),
                onInputPictureClicked = { }
            )
        }
    }
}