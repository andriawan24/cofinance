package id.andriawan24.cofinance.andro.ui.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogDatePickerContent(
    modifier: Modifier = Modifier,
    currentDate: Date,
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
                text = stringResource(R.string.label_date),
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
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null
            )
        }

        VerticalSpacing(Dimensions.SIZE_24)

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
                text = stringResource(R.string.label_time),
                style = MaterialTheme.typography.titleMedium
            )

            SecondaryButton(
                shape = MaterialTheme.shapes.medium,
                verticalPadding = Dimensions.SIZE_6,
                horizontalPadding = Dimensions.SIZE_12,
                onClick = onHourClicked
            ) {
                Text(
                    text = currentDate.formatToString("HH:mm z"),
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
                onSavedDate.invoke(datePickerState.selectedDateMillis ?: currentDate.time)
            }
        ) {
            Text(
                text = stringResource(R.string.action_choose_date),
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
            currentDate = Date(),
            onCloseDate = { },
            onSavedDate = { },
            onHourClicked = { },
            datePickerState = rememberDatePickerState()
        )
    }
}