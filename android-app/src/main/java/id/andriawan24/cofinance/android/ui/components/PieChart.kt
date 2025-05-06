package id.andriawan24.cofinance.android.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.PIXEL_2
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andriawan24.cofinance.android.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.ColorHelper
import id.andriawan24.cofinance.android.utils.Dimensions
import id.andriawan24.cofinance.android.utils.NumberHelper

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    data: Map<String, Int>,
    radiusOuter: Dp = Dimensions.SIZE_90,
    chartBarWidth: Dp = Dimensions.SIZE_20,
    animationDuration: Int = 1000,
    detailChart: @Composable ColumnScope.(data: Map<String, Int>, colors: List<Color>) -> Unit
) {
    val totalSum = data.values.sum()
    val floatValue = mutableListOf<Float>()
    val colors = ColorHelper.generateColorPalette(data.values.size)
    var animationPlayed by remember { mutableStateOf(false) }
    var lastValue = 0f
    val animateSize by animateFloatAsState(
        targetValue = if (animationPlayed) radiusOuter.value * 2.5f else 0f,
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

    data.values.forEachIndexed { index, values ->
        floatValue.add(index, 360 * values.toFloat() / totalSum.toFloat())
    }

    LaunchedEffect(true) {
        animationPlayed = true
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(animateSize.dp)
                .padding(chartBarWidth / 2),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(radiusOuter * 4)
                    .rotate(animateRotation)
            ) {
                floatValue.forEachIndexed { index, value ->
                    drawArc(
                        color = colors[index],
                        startAngle = lastValue,
                        sweepAngle = value,
                        useCenter = false,
                        style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Round)
                    )
                    lastValue += value
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_2)
            ) {
                Text(
                    text = "Total Income",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.typography.labelMedium.color.copy(alpha = 0.4f),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "IDR 100.000.000",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        detailChart(data, colors)
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
                        "Test" to 40000000,
                        "Test2" to 30000000,
                        "Test3" to 40000000,
                        "Test4" to 60000000,
                    ),
                    chartBarWidth = Dimensions.SIZE_18
                ) { data, colors ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimensions.SIZE_48),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
                    ) {
                        data.entries.forEachIndexed { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimensions.SIZE_8)
                                        .clip(CircleShape)
                                        .background(colors[index])
                                )

                                Text(
                                    text = item.key,
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = NumberHelper.formatRupiah(item.value),
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