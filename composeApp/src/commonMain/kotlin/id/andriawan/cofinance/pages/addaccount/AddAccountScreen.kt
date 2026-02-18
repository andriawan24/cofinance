package id.andriawan.cofinance.pages.addaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.ic_account
import cofinance.composeapp.generated.resources.ic_arrow_left
import cofinance.composeapp.generated.resources.ic_dropdown
import cofinance.composeapp.generated.resources.label_account_category
import cofinance.composeapp.generated.resources.label_add_account
import cofinance.composeapp.generated.resources.label_name
import cofinance.composeapp.generated.resources.label_rupiah
import cofinance.composeapp.generated.resources.label_zero
import id.andriawan.cofinance.components.AddNewSection
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.pages.addnew.AddAccountEvent
import id.andriawan.cofinance.pages.addnew.AddAccountUiState
import id.andriawan.cofinance.pages.addnew.AddAccountViewModel
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberFormatTransformation
import id.andriawan.cofinance.utils.enums.AccountGroupType
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddAccountScreen(onBackClicked: () -> Unit, onAddAccountSuccess: () -> Unit) {
    val addAccountViewModel: AddAccountViewModel = koinViewModel()
    val uiState by addAccountViewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }

    addAccountViewModel.accountAdded.CollectAsEffect {
        onAddAccountSuccess()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { contentPadding ->
        AddAccountContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            onEvent = { event ->
                when (event) {
                    is AddAccountEvent.BackClicked -> onBackClicked()
                    else -> addAccountViewModel.onEvent(event)
                }
            }
        )
    }
}

@Composable
fun AddAccountContent(
    modifier: Modifier = Modifier,
    uiState: AddAccountUiState,
    onEvent: (AddAccountEvent) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.SIZE_12, bottom = Dimensions.SIZE_24)
                .padding(horizontal = Dimensions.SIZE_16)
        ) {
            IconButton(
                onClick = { onEvent(AddAccountEvent.BackClicked) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_left),
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(Res.string.label_add_account),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_20))

        Box(modifier = Modifier.padding(horizontal = Dimensions.SIZE_16)) {
            AddNewSection(
                modifier = Modifier
                    .border(
                        width = Dimensions.SIZE_2,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large
                    ),
                label = stringResource(Res.string.label_account_category),
                value = uiState.category.displayName,
                onSectionClicked = { onEvent(AddAccountEvent.OpenCategoryChooser) },
                startIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_account),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                endIcon = {
                    Icon(
                        modifier = Modifier.rotate(-90f),
                        painter = painterResource(Res.drawable.ic_dropdown),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            )

            DropdownMenu(
                expanded = uiState.openCategoryChooser,
                onDismissRequest = { onEvent(AddAccountEvent.CloseCategoryChooser) },
                containerColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                AccountGroupType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        onClick = {
                            onEvent(AddAccountEvent.CategoryChosen(option))
                            onEvent(AddAccountEvent.CloseCategoryChooser)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                )
                .border(
                    width = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large
                )
                .padding(all = Dimensions.SIZE_16),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_10),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                modifier = Modifier.weight(1f),
                value = uiState.name,
                onValueChange = { onEvent(AddAccountEvent.NameChanged(it)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (uiState.name.isBlank()) {
                        Text(
                            text = stringResource(Res.string.label_name),
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

        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .border(
                    width = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large
                ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(all = Dimensions.SIZE_16)
            ) {
                Row {
                    Text(
                        text = stringResource(Res.string.label_rupiah),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    BasicTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.amount,
                        onValueChange = {
                            onEvent(AddAccountEvent.AmountChanged(it))
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        decorationBox = { innerTextField ->
                            if (uiState.amount.isBlank()) {
                                Text(
                                    text = stringResource(Res.string.label_zero),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            innerTextField()
                        },
                        visualTransformation = NumberFormatTransformation()
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = Dimensions.SIZE_24),
            onClick = { onEvent(AddAccountEvent.SaveAccount) },
            enabled = !uiState.isLoading
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
private fun AddAccountScreenPreview() {
    CofinanceTheme {
        Surface {
            AddAccountScreen(onBackClicked = { }, onAddAccountSuccess = { })
        }
    }
}
