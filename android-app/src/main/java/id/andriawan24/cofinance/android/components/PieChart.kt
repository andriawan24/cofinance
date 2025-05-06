package id.andriawan24.cofinance.android.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andriawan24.cofinance.android.utils.Dimensions

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

    data.values.forEachIndexed { index, values ->
        floatValue.add(index, 360 * values.toFloat() / totalSum.toFloat())
    }

    val colors = generateColorPalette(data.values.size)

    var animationPlayed by remember { mutableStateOf(false) }
    var lastValue = 0f

    val animateSize by animateFloatAsState(
        targetValue = if (animationPlayed) radiusOuter.value * 2f else 0f,
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
        Box(
            modifier = Modifier
                .size(animateSize.dp)
                .padding(chartBarWidth / 2),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(radiusOuter * 2f)
                    .rotate(animateRotation)
            ) {
                floatValue.forEachIndexed { index, value ->
                    drawArc(
                        color = colors[index],
                        startAngle = lastValue,
                        sweepAngle = value,
                        useCenter = false,
                        style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                    )

                    lastValue += value
                }
            }
        }

        detailChart(data, colors)
    }
}

private fun generateColorPalette(
    size: Int,
    baseColor: Color = Color(0xFF3B82F6), // Default to a blue color
    saturation: Float = 0.7f,
    brightness: Float = 0.9f
): List<Color> {
    if (size <= 0) return emptyList()

    // Simple conversion to get approximate hue
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
    val baseHue = hsv[0]

    val colors = mutableListOf<Color>()

    // Golden ratio conjugate to create well-distributed hues
    val goldenRatioConjugate = 0.618034f
    var hue = baseHue / 360f

    repeat(size) {
        // Use the golden ratio to generate evenly distributed hues
        hue += goldenRatioConjugate
        hue %= 1f

        // Convert HSV to RGB
        val hsv = FloatArray(3)
        hsv[0] = hue * 360f // Hue
        hsv[1] = saturation // Saturation
        hsv[2] = brightness // Value (brightness)

        val rgb = android.graphics.Color.HSVToColor(hsv)

        // Create and add the color to our list
        colors.add(
            Color(
                red = android.graphics.Color.red(rgb) / 255f,
                green = android.graphics.Color.green(rgb) / 255f,
                blue = android.graphics.Color.blue(rgb) / 255f,
                alpha = 1f
            )
        )
    }

    return colors
}