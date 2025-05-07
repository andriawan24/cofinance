package id.andriawan24.cofinance.andro.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object ColorHelper {
    fun generateColorPalette(
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
}