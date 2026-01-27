package id.andriawan.cofinance.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_account
import cofinance.composeapp.generated.resources.ic_chevron_right
import cofinance.composeapp.generated.resources.label_account
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.emptyString
import id.andriawan.cofinance.utils.extensions.conditional
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
            label = stringResource(Res.string.label_account),
            startIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_account),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}
