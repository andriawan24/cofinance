package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.ext.conditional

@Composable
fun AddNewSection(
    modifier: Modifier = Modifier,
    label: String,
    value: String = emptyString(),
    onSectionClicked: (() -> Unit)? = null,
    startIcon: @Composable () -> Unit,
    endIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
private fun AddNewSectionPreview() {
    CofinanceTheme {
        AddNewSection(
            label = stringResource(R.string.label_account),
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
    }
}