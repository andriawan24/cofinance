package id.andriawan24.cofinance.andro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.utils.TextSizes

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.Normal),
)

private val defaultTypography = Typography()

val cofinanceTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextSizes.SIZE_24
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = ManropeFontFamily
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = ManropeFontFamily
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = TextSizes.SIZE_16
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = ManropeFontFamily,
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextSizes.SIZE_16
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextSizes.SIZE_14
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = ManropeFontFamily
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = ManropeFontFamily
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = ManropeFontFamily
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextSizes.SIZE_20
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextSizes.SIZE_16
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextSizes.SIZE_12
    )
)