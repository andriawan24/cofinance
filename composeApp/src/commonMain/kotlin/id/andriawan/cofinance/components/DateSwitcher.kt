package id.andriawan24.cofinance.andro.ui.presentation.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_chevron_left
import cofinance.composeapp.generated.resources.ic_chevron_right
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource

@Composable
fun DateSwitcher(
    modifier: Modifier = Modifier,
    label: String,
    onPreviousClicked: () -> Unit,
    onNextClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .dropShadow(
                shape = ButtonDefaults.shape,
                shadow = Shadow(
                    radius = Dimensions.SIZE_10,
                    spread = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    offset = DpOffset(x = Dimensions.zero, y = Dimensions.SIZE_2)
                )
            )
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = Dimensions.SIZE_6),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                onClick = onPreviousClicked
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_left),
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            IconButton(
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                onClick = onNextClicked
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DateSwitcherPreview() {
    CofinanceTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimensions.SIZE_20)
        ) {
            DateSwitcher(
                modifier = Modifier.fillMaxWidth(),
                label = "May 2025",
                onNextClicked = {},
                onPreviousClicked = {}
            )
        }
    }
}
