package id.andriawan.cofinance.pages.account.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_add
import cofinance.composeapp.generated.resources.img_account_bg
import cofinance.composeapp.generated.resources.label_add_account
import cofinance.composeapp.generated.resources.label_rupiah
import cofinance.composeapp.generated.resources.label_total_assets
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssetCard(balance: Long, onAddAccountClicked: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = Dimensions.SIZE_16, y = -Dimensions.SIZE_20)
                    .size(Dimensions.SIZE_96)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))
            )

            Image(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Dimensions.SIZE_8),
                painter = painterResource(Res.drawable.img_account_bg),
                contentDescription = null
            )

            Column(
                modifier = Modifier.padding(Dimensions.SIZE_20),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)) {
                    Text(
                        text = stringResource(Res.string.label_total_assets),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Text(
                        text = "${stringResource(Res.string.label_rupiah)} ${
                            NumberHelper.formatNumber(
                                balance
                            )
                        }",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                SecondaryButton(
                    modifier = Modifier.sizeIn(minHeight = Dimensions.SIZE_44),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SIZE_16,
                        vertical = Dimensions.SIZE_10
                    ),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onAddAccountClicked,
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                    ) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.SIZE_16)
                        )

                        Text(
                            text = stringResource(Res.string.label_add_account),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AssetCardPreview() {
    CofinanceTheme {
        Surface {
            AssetCard(
                balance = 10_000_000,
                onAddAccountClicked = {

                }
            )
        }
    }
}
