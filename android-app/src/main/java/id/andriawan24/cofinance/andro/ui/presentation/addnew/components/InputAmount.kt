package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.isDigitsOnly
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberFormatTransformation

@Composable
fun InputAmount(
    modifier: Modifier = Modifier,
    amount: String,
    fee: String,
    includeFee: Boolean,
    enableFee: Boolean,
    onAmountChanged: (String) -> Unit,
    onFeeChanged: (String) -> Unit,
    onIncludeFeeChanged: (Boolean) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = amount,
                    onValueChange = {
                        if (it.isDigitsOnly() && it.length < 13) {
                            onAmountChanged(it)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        if (amount.isBlank()) {
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

            if (enableFee && !includeFee) {
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
                    onClick = {
                        onIncludeFeeChanged(true)
                    }
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
                            text = stringResource(R.string.label_fee),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (includeFee) {
            Row(
                modifier = Modifier
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
                    if (fee.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.label_rupiah),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    BasicTextField(
                        modifier = Modifier.weight(1f),
                        value = fee,
                        onValueChange = {
                            if (it.isDigitsOnly() && it.length < 13) {
                                onFeeChanged.invoke(it)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        decorationBox = { innerTextField ->
                            if (fee.isBlank()) {
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
                        .clickable { onIncludeFeeChanged(false) },
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview
@Composable
private fun InputAmountPreview() {
    CofinanceTheme {
        InputAmount(
            amount = "10000",
            fee = "1000",
            includeFee = false,
            onAmountChanged = { },
            enableFee = true,
            onFeeChanged = { },
            onIncludeFeeChanged = { }
        )
    }
}