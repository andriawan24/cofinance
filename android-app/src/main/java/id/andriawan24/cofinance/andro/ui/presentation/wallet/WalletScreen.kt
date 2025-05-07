package id.andriawan24.cofinance.andro.ui.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices.PIXEL_2_XL
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.TextSizes
import id.andriawan24.cofinance.andro.utils.ext.dropShadow

@Composable
fun WalletScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = Dimensions.SIZE_24)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ElevatedButton(
                onClick = {

                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dropShadow(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFF1E1E1E).copy(0.10f),
                    blur = Dimensions.SIZE_4,
                    offsetY = Dimensions.SIZE_2,
                    spread = Dimensions.zero
                )
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .clip(shape = MaterialTheme.shapes.medium)
                .padding(all = Dimensions.SIZE_20)
        ) {
            Column {
                Text(
                    text = "Total balance",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_2))

                Text(
                    text = "IDR 560.000.000",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = TextSizes.SIZE_32),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cash",
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = "IDR 20.000.000",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                modifier = Modifier.padding(top = Dimensions.SIZE_12),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(
                            horizontal = Dimensions.SIZE_12,
                            vertical = Dimensions.SIZE_8
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blu by BCA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    Text(
                        text = "IDR 20.000.000",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(
                            horizontal = Dimensions.SIZE_12,
                            vertical = Dimensions.SIZE_8
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blu by BCA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    Text(
                        text = "IDR 20.000.000",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(
                            horizontal = Dimensions.SIZE_12,
                            vertical = Dimensions.SIZE_8
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blu by BCA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    Text(
                        text = "IDR 20.000.000",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(Dimensions.SIZE_24))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cash",
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = "IDR 20.000.000",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                modifier = Modifier.padding(top = Dimensions.SIZE_12),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(
                            horizontal = Dimensions.SIZE_12,
                            vertical = Dimensions.SIZE_8
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blu by BCA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    Text(
                        text = "IDR 20.000.000",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(
                            horizontal = Dimensions.SIZE_12,
                            vertical = Dimensions.SIZE_8
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blu by BCA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    Text(
                        text = "IDR 20.000.000",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF1E1E1E).copy(0.10f),
                            blur = Dimensions.SIZE_8,
                            offsetY = Dimensions.SIZE_2,
                            spread = Dimensions.zero
                        )
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(shape = MaterialTheme.shapes.small)
                        .padding(
                            horizontal = Dimensions.SIZE_12,
                            vertical = Dimensions.SIZE_8
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blu by BCA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    Text(
                        text = "IDR 20.000.000",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(device = PIXEL_2_XL, showBackground = true)
@Composable
private fun WalletScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            WalletScreen()
        }
    }
}