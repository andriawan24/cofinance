package id.andriawan.cofinance.components

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andriawan.cofinance.pages.stats.StatItem
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.NumberHelper
import id.andriawan.cofinance.utils.enums.TransactionCategory
import org.jetbrains.compose.resources.stringResource

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    totalExpenses: Long,
    data: List<StatItem>,
    radiusOuter: Dp = Dimensions.SIZE_120,
    chartBarWidth: Dp = Dimensions.SIZE_20,
    animationDuration: Int = 1000,
    detailChart: @Composable ColumnScope.(data: List<StatItem>) -> Unit
) {
    var animationPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

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
                    data.forEach { value ->
                        drawArc(
                            color = value.category.iconColor,
                            startAngle = lastValue,
                            sweepAngle = if (value == data.last()) value.sweepAngle else value.sweepAngle - 2f,
                            useCenter = false,
                            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                        )
                        lastValue += value.sweepAngle
                        if (data.size > 1) {
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
            }

            Spacer(Modifier.width(Dimensions.SIZE_24))

            Column {
                Text(
                    text = "Total Expenses",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = NumberHelper.formatRupiah(totalExpenses),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        detailChart(data)
    }
}

@Composable
private fun PieChartPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(Dimensions.SIZE_24)) {
                PieChart(
                    totalExpenses = 100_000_000,
                    data = listOf(
                        StatItem(
                            category = TransactionCategory.SUBSCRIPTION,
                            amount = 100_000,
                            percentage = 50.0f,
                            sweepAngle = 180f,
                        ),
                        StatItem(
                            category = TransactionCategory.SUBSCRIPTION,
                            amount = 100_000,
                            percentage = 50.0f,
                            sweepAngle = 180f,
                        )
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
                                    modifier = Modifier.size(Dimensions.SIZE_8)
                                        .clip(CircleShape)
                                        .background(item.category.color)
                                )

                                Text(
                                    text = stringResource(item.category.label),
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = NumberHelper.formatRupiah(item.amount),
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
