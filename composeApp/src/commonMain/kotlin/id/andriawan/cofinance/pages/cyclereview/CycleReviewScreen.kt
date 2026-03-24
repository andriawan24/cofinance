package id.andriawan.cofinance.pages.cyclereview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_confirm
import cofinance.composeapp.generated.resources.carry_all
import cofinance.composeapp.generated.resources.carry_over_no
import cofinance.composeapp.generated.resources.carry_over_yes
import cofinance.composeapp.generated.resources.cycle_reset_subtitle
import cofinance.composeapp.generated.resources.cycle_reset_title
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CycleReviewScreen(
    onCompleted: () -> Unit,
    cycleReviewViewModel: CycleReviewViewModel = koinViewModel()
) {
    val uiState by cycleReviewViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onCompleted()
        }
    }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(Dimensions.SIZE_24)
        ) {
            Text(
                text = stringResource(Res.string.cycle_reset_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

            Text(
                text = stringResource(Res.string.cycle_reset_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                items(uiState.accounts) { choice ->
                    AccountCarryOverCard(
                        choice = choice,
                        onToggle = { cycleReviewViewModel.toggleCarryOver(choice.account.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                SecondaryButton(
                    modifier = Modifier.weight(1f),
                    onClick = { cycleReviewViewModel.carryAll() }
                ) {
                    Text(
                        text = stringResource(Res.string.carry_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    onClick = { cycleReviewViewModel.confirm() },
                    enabled = !uiState.isProcessing
                ) {
                    Text(
                        text = stringResource(Res.string.action_confirm),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCarryOverCard(
    choice: AccountCarryOverChoice,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = Dimensions.SIZE_1,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(Dimensions.SIZE_16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = choice.account.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Text(
                        text = choice.account.group.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Text(
                    text = NumberHelper.formatRupiah(choice.account.balance),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (choice.account.balance >= 0)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.error
                    )
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = choice.carryOver,
                        onClick = { if (!choice.carryOver) onToggle() }
                    )
                    Text(
                        text = stringResource(Res.string.carry_over_yes),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = !choice.carryOver,
                        onClick = { if (choice.carryOver) onToggle() }
                    )
                    Text(
                        text = stringResource(Res.string.carry_over_no),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
