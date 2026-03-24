package id.andriawan.cofinance.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.ic_close
import cofinance.composeapp.generated.resources.label_edit_account
import cofinance.composeapp.generated.resources.label_name
import cofinance.composeapp.generated.resources.label_rupiah
import cofinance.composeapp.generated.resources.label_zero
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberFormatTransformation
import id.andriawan.cofinance.utils.enums.AccountType
import id.andriawan.cofinance.utils.extensions.isDigitOnly
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditAccountBottomSheetContent(
    account: Account,
    onSaveClicked: (name: String, balance: Long, accountType: AccountType) -> Unit,
    onCloseClicked: () -> Unit
) {
    var name by remember(account) { mutableStateOf(account.name) }
    var amount by remember(account) { mutableStateOf(account.balance.toString()) }
    var selectedType by remember(account) { mutableStateOf(account.accountType) }

    Column {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = stringResource(Res.string.label_edit_account),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Image(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable { onCloseClicked() },
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        // Account Type selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_10)
        ) {
            AccountType.entries.forEach { type ->
                val isSelected = selectedType == type
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = type },
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onPrimary,
                    border = BorderStroke(
                        width = Dimensions.SIZE_2,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(all = Dimensions.SIZE_16),
                        text = type.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onBackground
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

        // Name field
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
                value = name,
                onValueChange = { name = it },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (name.isBlank()) {
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

        // Balance field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .border(
                    width = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large
                )
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
                        value = amount,
                        onValueChange = {
                            if (it.isDigitOnly() && it.length < 13) {
                                amount = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        decorationBox = { innerTextField ->
                            if (amount.isBlank()) {
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

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(bottom = Dimensions.SIZE_24),
            enabled = name.isNotBlank(),
            onClick = {
                onSaveClicked(
                    name,
                    amount.toLongOrNull() ?: 0L,
                    selectedType
                )
            }
        ) {
            Text(
                text = stringResource(Res.string.action_save),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
