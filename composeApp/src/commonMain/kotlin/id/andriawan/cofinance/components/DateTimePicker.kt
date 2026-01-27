package id.andriawan.cofinance.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_choose_date
import cofinance.composeapp.generated.resources.ic_close
import cofinance.composeapp.generated.resources.label_date
import cofinance.composeapp.generated.resources.label_time
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.extensions.formatToString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogDatePickerContent(
    modifier: Modifier = Modifier,
    currentDate: String,
    datePickerState: DatePickerState,
    onCloseDate: () -> Unit,
    onHourClicked: () -> Unit,
    onSavedDate: (date: Long) -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
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
                text = stringResource(Res.string.label_date),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Image(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable { onCloseDate() },
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                navigationContentColor = MaterialTheme.colorScheme.primary,
                dayContentColor = MaterialTheme.colorScheme.onBackground
            ),
            showModeToggle = false,
            title = null,
            headline = null
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(bottom = Dimensions.SIZE_24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.label_time),
                style = MaterialTheme.typography.titleMedium
            )

            SecondaryButton(
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(
                    vertical = Dimensions.SIZE_6,
                    horizontal = Dimensions.SIZE_12
                ),
                onClick = onHourClicked
            ) {
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(bottom = Dimensions.SIZE_24),
            onClick = {
                onSavedDate.invoke(datePickerState.selectedDateMillis ?: Clock.System.now().toEpochMilliseconds())
            }
        ) {
            Text(
                text = stringResource(Res.string.action_choose_date),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DatePickerPreview() {
    CofinanceTheme {
        DialogDatePickerContent(
            currentDate = Clock.System.now().formatToString(),
            onCloseDate = { },
            onSavedDate = { },
            onHourClicked = { },
            datePickerState = rememberDatePickerState()
        )
    }
}
