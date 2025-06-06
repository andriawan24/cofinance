package id.andriawan24.cofinance.andro.utils.ext

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import id.andriawan24.cofinance.andro.utils.Dimensions

fun Modifier.dropShadow(
    shape: Shape,
    color: Color = Color.Black.copy(0.25f),
    blur: Dp = Dimensions.SIZE_4,
    offsetY: Dp = Dimensions.SIZE_4,
    offsetX: Dp = Dimensions.zero,
    spread: Dp = Dimensions.zero
) = this.drawBehind {
    val shadowSize = Size(size.width + spread.toPx(), size.height + spread.toPx())
    val shadowOutline = shape.createOutline(shadowSize, layoutDirection, this)

    val paint = Paint().apply {
        this.color = color
        if (blur.toPx() > 0) {
            this.asFrameworkPaint().apply {
                maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    drawIntoCanvas { canvas ->
        canvas.run {
            save()
            translate(offsetX.toPx(), offsetY.toPx())
            drawOutline(shadowOutline, paint)
            restore()
        }
    }
}

@Composable
@Stable
fun Modifier.conditional(
    condition: Boolean,
    trueModifier: @Composable Modifier.() -> Modifier,
    falseModifier: (Modifier.() -> Modifier)? = null
): Modifier {
    return if (condition) {
        then(trueModifier(Modifier))
    } else {
        if (falseModifier != null) {
            then(falseModifier(Modifier))
        } else {
            this
        }
    }
}