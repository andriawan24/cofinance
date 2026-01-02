package id.andriawan.cofinance.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.manrope
import cofinance.composeapp.generated.resources.manrope_bold
import cofinance.composeapp.generated.resources.manrope_extrabold
import cofinance.composeapp.generated.resources.manrope_medium
import cofinance.composeapp.generated.resources.manrope_semibold
import id.andriawan.cofinance.utils.TextSizes
import org.jetbrains.compose.resources.Font

@Composable
fun manropeFontFamily() = FontFamily(
    Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
    Font(Res.font.manrope_bold, FontWeight.Bold),
    Font(Res.font.manrope_semibold, FontWeight.SemiBold),
    Font(Res.font.manrope_medium, FontWeight.Medium),
    Font(Res.font.manrope, FontWeight.Normal),
)

@Composable
fun cofinanceTypography(): Typography {
    val defaultTypography = Typography()

    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold
        ),
        displayMedium = defaultTypography.displayMedium.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_28
        ),
        displaySmall = defaultTypography.displaySmall.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_24
        ),
        headlineLarge = defaultTypography.headlineLarge.copy(
            fontFamily = manropeFontFamily()
        ),
        headlineMedium = defaultTypography.headlineMedium.copy(
            fontFamily = manropeFontFamily()
        ),
        headlineSmall = defaultTypography.headlineSmall.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = TextSizes.SIZE_16
        ),
        titleLarge = defaultTypography.titleLarge.copy(
            fontFamily = manropeFontFamily(),
            fontSize = TextSizes.SIZE_20,
            fontWeight = FontWeight.SemiBold
        ),
        titleMedium = defaultTypography.titleMedium.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_16
        ),
        titleSmall = defaultTypography.titleSmall.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_14
        ),
        bodyLarge = defaultTypography.bodyLarge.copy(
            fontFamily = manropeFontFamily()
        ),
        bodyMedium = defaultTypography.bodyMedium.copy(
            fontFamily = manropeFontFamily(),
            fontSize = TextSizes.SIZE_14
        ),
        bodySmall = defaultTypography.bodySmall.copy(
            fontFamily = manropeFontFamily()
        ),
        labelLarge = defaultTypography.labelLarge.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_20
        ),
        labelMedium = defaultTypography.labelMedium.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_16
        ),
        labelSmall = defaultTypography.labelSmall.copy(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.SIZE_12
        )
    )
}