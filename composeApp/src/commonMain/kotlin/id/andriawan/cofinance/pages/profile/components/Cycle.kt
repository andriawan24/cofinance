package id.andriawan.cofinance.pages.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_calendar
import cofinance.composeapp.generated.resources.ic_chevron_right
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_cycle_start_day
import cofinance.composeapp.generated.resources.label_cycle_start_day_description
import cofinance.composeapp.generated.resources.label_yes
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CycleStartDaySetting(
    modifier: Modifier = Modifier,
    cycleStartDay: Int,
    isUpdating: Boolean,
    onDaySelected: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(enabled = !isUpdating) { showPicker = true }
                .semantics { role = Role.Button }
                .padding(Dimensions.SIZE_16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.SIZE_44)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(Dimensions.SIZE_20),
                    painter = painterResource(Res.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.label_cycle_start_day),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(Res.string.label_cycle_start_day_description),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = Dimensions.SIZE_12, vertical = Dimensions.SIZE_8),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cycleStartDay.toString(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.SIZE_20),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = Dimensions.SIZE_2
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPicker) {
        CycleStartDayPickerDialog(
            currentDay = cycleStartDay,
            onDaySelected = { day ->
                showPicker = false
                if (day != cycleStartDay) {
                    onDaySelected(day)
                }
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun CycleStartDayPickerDialog(
    currentDay: Int,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(currentDay) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.SIZE_24),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20)
            ) {
                CycleStartDayHeader()

                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = GridCells.Adaptive(minSize = Dimensions.SIZE_44),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                    userScrollEnabled = false
                ) {
                    items((1..28).toList()) { day ->
                        CycleStartDayCell(
                            day = day,
                            isSelected = day == selectedDay,
                            onClick = { selectedDay = day }
                        )
                    }
                }

                CycleStartDayActions(
                    onDismiss = onDismiss,
                    onConfirm = { onDaySelected(selectedDay) }
                )
            }
        }
    }
}

@Composable
private fun CycleStartDayHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)) {
        Text(
            text = stringResource(Res.string.label_cycle_start_day),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(Res.string.label_cycle_start_day_description),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun CycleStartDayCell(
    day: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = Dimensions.SIZE_44,
                minHeight = Dimensions.SIZE_44
            )
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = backgroundColor, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        )
    }
}

@Composable
private fun CycleStartDayActions(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
    ) {
        SecondaryButton(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = Dimensions.SIZE_44),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onDismiss
        ) {
            Text(
                text = stringResource(Res.string.label_cancel),
                style = MaterialTheme.typography.labelMedium
            )
        }

        PrimaryButton(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = Dimensions.SIZE_44),
            onClick = onConfirm,
            horizontalPadding = Dimensions.SIZE_16,
            verticalPadding = Dimensions.SIZE_12
        ) {
            Text(
                text = stringResource(Res.string.label_yes),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun CycleStartDaySettingPreview() {
    CofinanceTheme {
        Surface {
            CycleStartDaySetting(
                cycleStartDay = 12,
                isUpdating = false,
                onDaySelected = { }
            )
        }
    }
}

@Preview
@Composable
private fun CycleStartDaySettingUpdatingPreview() {
    CofinanceTheme {
        Surface {
            CycleStartDaySetting(
                cycleStartDay = 12,
                isUpdating = true,
                onDaySelected = { }
            )
        }
    }
}

@Preview
@Composable
private fun CycleStartDayHeaderPreview() {
    CofinanceTheme {
        Surface {
            CycleStartDayHeader()
        }
    }
}

@Preview
@Composable
private fun CycleStartDayActionsPreview() {
    CofinanceTheme {
        Surface {
            CycleStartDayActions(
                onDismiss = { },
                onConfirm = { }
            )
        }
    }
}
