package id.andriawan.cofinance.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_close
import cofinance.composeapp.generated.resources.ic_plus
import cofinance.composeapp.generated.resources.ic_two_coins
import cofinance.composeapp.generated.resources.label_fee
import cofinance.composeapp.generated.resources.label_rupiah
import cofinance.composeapp.generated.resources.label_zero
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberFormatTransformation
import id.andriawan.cofinance.utils.extensions.isDigitOnly
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
        AmountInputSection(
            amount = amount,
            showAddFeeButton = enableFee && !includeFee,
            onAmountChanged = onAmountChanged,
            onAddFeeClick = { onIncludeFeeChanged(true) }
        )

        if (includeFee) {
            FeeInputSection(
                fee = fee,
                onFeeChanged = onFeeChanged,
                onRemoveFeeClick = { onIncludeFeeChanged(false) }
            )
        }
    }
}

@Composable
private fun AmountInputSection(
    amount: String,
    showAddFeeButton: Boolean,
    onAmountChanged: (String) -> Unit,
    onAddFeeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .padding(all = Dimensions.SIZE_16)
    ) {
        AmountTextField(
            amount = amount,
            onAmountChanged = onAmountChanged
        )

        if (showAddFeeButton) {
            AddFeeButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onAddFeeClick
            )
        }
    }
}

@Composable
private fun AmountTextField(
    amount: String,
    onAmountChanged: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)) {
        Text(
            text = stringResource(Res.string.label_rupiah),
            style = MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { newValue ->
                if ((newValue.isBlank() || newValue.isDigitOnly()) && newValue.length < 13) {
                    onAmountChanged(newValue)
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
                AmountPlaceholder(showPlaceholder = amount.isBlank())
                innerTextField()
            },
            visualTransformation = NumberFormatTransformation()
        )
    }
}

@Composable
private fun AmountPlaceholder(showPlaceholder: Boolean) {
    if (showPlaceholder) {
        Text(
            text = stringResource(Res.string.label_zero),
            style = MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun AddFeeButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier,
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
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_plus),
                contentDescription = null
            )

            Text(
                text = stringResource(Res.string.label_fee),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun FeeInputSection(
    fee: String,
    onFeeChanged: (String) -> Unit,
    onRemoveFeeClick: () -> Unit
) {
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
            painter = painterResource(Res.drawable.ic_two_coins),
            contentDescription = null
        )

        FeeTextField(
            modifier = Modifier.weight(1f),
            fee = fee,
            onFeeChanged = onFeeChanged
        )

        Image(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onRemoveFeeClick() },
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = null
        )
    }
}

@Composable
private fun FeeTextField(
    modifier: Modifier = Modifier,
    fee: String,
    onFeeChanged: (String) -> Unit
) {
    Row(modifier = modifier) {
        if (fee.isNotBlank()) {
            Text(
                text = stringResource(Res.string.label_rupiah),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        BasicTextField(
            modifier = Modifier.weight(1f),
            value = fee,
            onValueChange = { newValue ->
                if ((newValue.isBlank() || newValue.isDigitOnly()) && newValue.length < 13) {
                    onFeeChanged(newValue)
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
                FeePlaceholder(showPlaceholder = fee.isBlank())
                innerTextField()
            },
            visualTransformation = NumberFormatTransformation()
        )
    }
}

@Composable
private fun FeePlaceholder(showPlaceholder: Boolean) {
    if (showPlaceholder) {
        Text(
            text = stringResource(Res.string.label_fee),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
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
