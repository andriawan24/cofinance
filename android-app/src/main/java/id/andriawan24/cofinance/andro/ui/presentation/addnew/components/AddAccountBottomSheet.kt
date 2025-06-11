package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddAccountViewModel
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberFormatTransformation
import id.andriawan24.cofinance.andro.utils.enums.AccountGroupType
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddAccountBottomSheet(onAccountSaved: () -> Unit, onCloseClicked: () -> Unit) {
    val addAccountViewModel: AddAccountViewModel = koinViewModel()
    val uiState by addAccountViewModel.uiState.collectAsStateWithLifecycle()

    addAccountViewModel.closeBottomSheet.CollectAsEffect {
        onAccountSaved()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = stringResource(R.string.label_add_account),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Image(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable {
                        onCloseClicked()
                    },
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null
            )
        }

        VerticalSpacing(size = Dimensions.SIZE_20)

        Box(modifier = Modifier.padding(horizontal = Dimensions.SIZE_16)) {
            AddNewSection(
                modifier = Modifier
                    .border(
                        width = Dimensions.SIZE_2,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large
                    ),
                label = stringResource(R.string.label_account_category),
                value = uiState.category?.displayName.orEmpty(),
                onSectionClicked = {
                    addAccountViewModel.openCategoryChooser()
                },
                startIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_account),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                endIcon = {
                    Icon(
                        modifier = Modifier.rotate(-90f),
                        painter = painterResource(R.drawable.ic_dropdown),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            )

            DropdownMenu(
                expanded = uiState.openCategoryChooser,
                onDismissRequest = { addAccountViewModel.closeCategoryChooser() },
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
                            addAccountViewModel.run {
                                onCategorySelected(option)
                                closeCategoryChooser()
                            }
                        }
                    )
                }
            }
        }

        VerticalSpacing(size = Dimensions.SIZE_16)

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
                onValueChange = addAccountViewModel::onNameChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (uiState.name.isBlank()) {
                        Text(
                            text = stringResource(R.string.label_name),
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

        VerticalSpacing(size = Dimensions.SIZE_16)

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
                        text = stringResource(R.string.label_rupiah),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    BasicTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.amount,
                        onValueChange = {
                            if (it.isDigitsOnly() && it.length < 13) {
                                addAccountViewModel.onAmountChanged(it)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        decorationBox = { innerTextField ->
                            if (uiState.amount.isBlank()) {
                                Text(
                                    text = stringResource(R.string.label_zero),
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

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = Dimensions.SIZE_24),
            onClick = addAccountViewModel::saveAccount,
            enabled = true
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
private fun AddAccountBottomSheetPreview() {
    CofinanceTheme {
        Surface(color = MaterialTheme.colorScheme.onPrimary) {
            AddAccountBottomSheet(
                onAccountSaved = {},
                onCloseClicked = {}
            )
        }
    }
}