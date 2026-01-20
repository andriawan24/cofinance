package id.andriawan.cofinance.pages.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.domain.model.response.toDrawable
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AccountGroupCard(
    modifier: Modifier = Modifier,
    group: AccountByGroup,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SIZE_16)
            .dropShadow(
                shape = MaterialTheme.shapes.large,
                shadow = Shadow(
                    radius = Dimensions.SIZE_10,
                    spread = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    offset = DpOffset(x = Dimensions.zero, y = -Dimensions.SIZE_4)
                )
            )
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .padding(Dimensions.SIZE_16)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = Dimensions.SIZE_12)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(group.backgroundColor),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(all = Dimensions.SIZE_12),
            ) {
                Image(
                    painter = painterResource(group.accountGroupType.toDrawable()),
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = group.groupLabel,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = NumberHelper.formatRupiah(group.totalAmount),
                style = MaterialTheme.typography.labelMedium
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Dimensions.SIZE_16),
            thickness = Dimensions.SIZE_1,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)) {
            group.accounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .background(
                            color = Color(group.backgroundColor),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(all = Dimensions.SIZE_12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = Dimensions.SIZE_12)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Text(
                        text = NumberHelper.formatRupiah(number = account.balance),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
