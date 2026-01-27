package id.andriawan.cofinance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_add_new_account
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.ic_close
import cofinance.composeapp.generated.resources.ic_dropdown
import cofinance.composeapp.generated.resources.label_account
import cofinance.composeapp.generated.resources.label_loading_accounts
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.pages.addnew.AddNewUiState
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Immutable
data class AccountGroup(
    val name: String,
    val accounts: List<Account>
)

@Composable
fun AccountBottomSheetContent(
    uiState: AddNewUiState,
    onAccountSaved: (Account) -> Unit,
    onAddAccountClicked: () -> Unit,
    onCloseClicked: () -> Unit
) {
    var shownDropdown by remember { mutableIntStateOf(-1) }
    var currentSelectedAccount by remember(uiState.senderAccount) {
        mutableStateOf(uiState.senderAccount)
    }

    Column {
        AccountBottomSheetHeader(onCloseClicked = onCloseClicked)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = Dimensions.SIZE_24)
        ) {
            if (uiState.isLoadingAccount) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimensions.SIZE_48),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
                        ) {
                            CircularProgressIndicator()

                            Text(
                                text = stringResource(Res.string.label_loading_accounts),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            if (!uiState.isLoadingAccount) {
                itemsIndexed(uiState.accounts) { index, group ->
                    AccountGroupItem(
                        accountGroup = AccountGroup(
                            name = group.groupLabel,
                            accounts = group.accounts
                        ),
                        isExpanded = index == shownDropdown,
                        selectedAccount = currentSelectedAccount,
                        onToggleExpanded = {
                            shownDropdown = if (shownDropdown == index) -1 else index
                        },
                        onAccountSelected = { account ->
                            currentSelectedAccount = account
                        }
                    )

                    if (index != uiState.accounts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
                            thickness = Dimensions.SIZE_1,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    }
                }

                item {
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimensions.SIZE_24)
                            .padding(horizontal = Dimensions.SIZE_24),
                        onClick = onAddAccountClicked
                    ) {
                        Text(text = stringResource(Res.string.action_add_new_account))
                    }
                }
            }
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.SIZE_16,
                    vertical = Dimensions.SIZE_24
                ),
            enabled = currentSelectedAccount != null,
            onClick = { currentSelectedAccount?.let(onAccountSaved) }
        ) {
            Text(
                text = stringResource(Res.string.action_save),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// Extracted components
@Composable
private fun AccountBottomSheetHeader(
    onCloseClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_16)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            text = stringResource(Res.string.label_account),
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
}

@Composable
private fun AccountItem(
    account: Account,
    isSelected: Boolean,
    onAccountSelected: (Account) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimensions.SIZE_32,
                end = Dimensions.SIZE_20
            )
            .padding(vertical = Dimensions.SIZE_14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = account.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        RadioButton(
            selected = isSelected,
            colors = RadioButtonDefaults.colors(
                unselectedColor = MaterialTheme.colorScheme.primary
            ),
            onClick = { onAccountSelected(account) }
        )
    }
}

@Composable
private fun AccountGroupItem(
    accountGroup: AccountGroup,
    isExpanded: Boolean,
    selectedAccount: Account?,
    onToggleExpanded: () -> Unit,
    onAccountSelected: (Account) -> Unit
) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "animate dropdown icon"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(all = Dimensions.SIZE_16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = accountGroup.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Icon(
                modifier = Modifier.rotate(animatedRotation),
                painter = painterResource(Res.drawable.ic_dropdown),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                accountGroup.accounts.forEach { account ->
                    AccountItem(
                        account = account,
                        isSelected = selectedAccount == account,
                        onAccountSelected = onAccountSelected
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AccountBottomSheetContentPreview() {
    CofinanceTheme {
        Surface {
            AccountBottomSheetContent(
                uiState = AddNewUiState(),
                onAccountSaved = { },
                onCloseClicked = { },
                onAddAccountClicked = { }
            )
        }
    }
}
