package id.andriawan24.cofinance.andro.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.PIXEL_2
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.NumberHelper
import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory
import kotlin.math.roundToInt

typealias PieLabelType = Triple<TransactionCategory, Long, Int>

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    data: Map<TransactionCategory, Long>,
    radiusOuter: Dp = Dimensions.SIZE_120,
    chartBarWidth: Dp = Dimensions.SIZE_20,
    animationDuration: Int = 1000,
    detailChart: @Composable ColumnScope.(data: List<PieLabelType>) -> Unit
) {
    val totalSum = remember(data) { data.values.sum() }
    val arcData = remember(data) {
        data.entries.map { entry ->
            val sweepAngle = 360 * entry.value / totalSum.toFloat()
            Pair(entry.key.iconColor, sweepAngle)
        }
    }

    val labelData = remember(data) {
        data.entries.map { entry ->
            val sweepAngle = 360 * entry.value / totalSum.toFloat()
            PieLabelType(entry.key, entry.value, ((sweepAngle / 360) * 100).roundToInt())
        }
    }

    var animationPlayed by remember { mutableStateOf(false) }

    val animateSize by animateFloatAsState(
        targetValue = if (animationPlayed) radiusOuter.value + chartBarWidth.value else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )

    val animateRotation by animateFloatAsState(
        targetValue = if (animationPlayed) 90f * 11f else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )

    LaunchedEffect(true) {
        animationPlayed = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(animateSize.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(radiusOuter)
                        .rotate(animateRotation)
                ) {
                    var lastValue = 0f
                    arcData.forEach { value ->
                        drawArc(
                            color = value.first,
                            startAngle = lastValue,
                            sweepAngle = if (value == arcData.last()) value.second else value.second - 2f,
                            useCenter = false,
                            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                        )
                        lastValue += value.second
                        drawArc(
                            color = Color.White,
                            startAngle = lastValue,
                            sweepAngle = 2f,
                            useCenter = false,
                            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                }
            }

            HorizontalSpacing(Dimensions.SIZE_24)

            Column {
                Text(
                    text = "Total Expenses",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = NumberHelper.formatRupiah(totalSum),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        detailChart(labelData)
    }
}

@Preview(showBackground = true, device = PIXEL_2)
@Composable
private fun PieChartPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(Dimensions.SIZE_24)) {
                PieChart(
                    data = mapOf(
                        TransactionCategory.SUBSCRIPTION to 40000000,
                        TransactionCategory.FOOD to 40000000,
                        TransactionCategory.ADMINISTRATION to 30000000,
                        TransactionCategory.APPAREL to 40000000,
                        TransactionCategory.EDUCATION to 60000000,
                    )
                ) { data ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimensions.SIZE_48),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
                    ) {
                        data.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimensions.SIZE_8)
                                        .clip(CircleShape)
                                        .background(item.first.iconColor)
                                )

                                Text(
                                    text = stringResource(item.first.labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = NumberHelper.formatRupiah(item.second),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}