package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.titlecase
import id.andriawan24.cofinance.domain.model.response.Account

@Composable
fun AccountBottomSheet(
    isLoading: Boolean,
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSaved: (Account) -> Unit,
    onAddAccountClicked: () -> Unit,
    onCloseClicked: () -> Unit
) {
    var shownDropdown by remember { mutableIntStateOf(-1) }
    var currentSelectedAccount by remember { mutableStateOf(selectedAccount) }
    val accountByGroups = remember { accounts.groupBy { it.group }.toList() }

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
                text = stringResource(R.string.label_account),
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


            itemsIndexed(accountByGroups) { index, data ->
                val animatedRotation by animateFloatAsState(
                    targetValue = if (shownDropdown == index) 180f else 0f,
                    label = "animate dropdown icon"
                )

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shownDropdown = if (shownDropdown == index) -1 else index }
                            .padding(all = Dimensions.SIZE_16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = data.first.titlecase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        Icon(
                            modifier = Modifier.rotate(animatedRotation),
                            painter = painterResource(R.drawable.ic_dropdown),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }

                    AnimatedVisibility(visible = index == shownDropdown) {
                        Column {
                            data.second.forEach { account ->
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
                                        selected = currentSelectedAccount == account,
                                        colors = RadioButtonDefaults.colors(
                                            unselectedColor = MaterialTheme.colorScheme.primary
                                        ),
                                        onClick = {
                                            currentSelectedAccount = account
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (index != accounts.groupBy { it.group }.toList().lastIndex) {
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
                    Text("Add new account")
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
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun AccountBottomSheetPreview() {
    CofinanceTheme {
        Surface {
            AccountBottomSheet(
                isLoading = false,
                accounts = emptyList(),
                selectedAccount = null,
                onAccountSaved = { },
                onCloseClicked = { },
                onAddAccountClicked = {}
            )
        }
    }
}