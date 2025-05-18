package id.andriawan24.cofinance.andro.ui.presentation.addexpenses.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberFormatTransformation
import id.andriawan24.cofinance.andro.utils.ext.conditional

@Composable
fun ExpenseSection(modifier: Modifier = Modifier, onInputPictureClicked: () -> Unit) {
    var inputAmount by remember { mutableStateOf("") }
    var inputFee by remember { mutableStateOf("") }
    var includeFee by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        InputPicture(onInputPictureClicked = onInputPictureClicked)

        InputAmount(
            inputAmount = inputAmount,
            includeFee = includeFee,
            onIncludeFeeClicked = { includeFee = true },
            onInputAmountChanged = { inputAmount = it }
        )

        if (includeFee) {
            InputFee(
                inputFee = inputFee,
                onInputFeeChanged = { inputFee = it },
                onCloseFeeClicked = { includeFee = false }
            )
        }

        AddNewSection(
            label = "Account",
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
            onSectionClicked = {

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
            label = "Dates",
            value = "",
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        InputNote(
            note = "",
            onNoteChanged = {}
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
}

@Composable
fun InputPicture(onInputPictureClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_16
            )
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large
            )
            .border(
                border = BorderStroke(
                    width = Dimensions.SIZE_1,
                    color = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.large
            )
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(true) { onInputPictureClicked() }
            .padding(Dimensions.SIZE_16),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Auto-Fill with Receipt Photo",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "Upload your receipt and we’ll fill it in!",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Box(modifier = Modifier.padding(Dimensions.SIZE_6)) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null
            )
        }
    }
}

@Composable
fun InputAmount(
    inputAmount: String,
    includeFee: Boolean,
    onInputAmountChanged: (String) -> Unit,
    onIncludeFeeClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_16)
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .padding(all = Dimensions.SIZE_16)
    ) {
        Row {
            Text(
                text = stringResource(R.string.label_rupiah),
                style = MaterialTheme.typography.displayMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = inputAmount,
                onValueChange = onInputAmountChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (inputAmount.isBlank()) {
                        Text(
                            text = stringResource(R.string.label_zero),
                            style = MaterialTheme.typography.displayMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    innerTextField()
                },
                visualTransformation = NumberFormatTransformation()
            )
        }

        if (!includeFee) {
            OutlinedButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                contentPadding = PaddingValues(
                    horizontal = Dimensions.SIZE_16,
                    vertical = Dimensions.SIZE_4
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                border = BorderStroke(
                    width = Dimensions.SIZE_1,
                    color = MaterialTheme.colorScheme.primary
                ),
                onClick = onIncludeFeeClicked
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null
                    )

                    Text(
                        text = "Fee",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun InputFee(inputFee: String, onInputFeeChanged: (String) -> Unit, onCloseFeeClicked: () -> Unit) {
    Row(
        modifier = Modifier
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
            painter = painterResource(R.drawable.ic_two_coins),
            contentDescription = null
        )

        Row(modifier = Modifier.weight(1f)) {
            if (inputFee.isNotBlank()) {
                Text(
                    text = stringResource(R.string.label_rupiah),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            BasicTextField(
                modifier = Modifier.weight(1f),
                value = inputFee,
                onValueChange = onInputFeeChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (inputFee.isBlank()) {
                        Text(
                            text = stringResource(R.string.label_fee),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    innerTextField()
                },
                visualTransformation = NumberFormatTransformation()
            )
        }

        Image(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onCloseFeeClicked() },
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null
        )
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
            if (note.isNotBlank()) {
                Text(
                    text = stringResource(R.string.label_rupiah),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

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
                },
                visualTransformation = NumberFormatTransformation()
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
            ExpenseSection(onInputPictureClicked = {})
        }
    }
}