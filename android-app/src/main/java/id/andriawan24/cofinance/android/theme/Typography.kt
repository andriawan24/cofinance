package id.andriawan24.cofinance.android.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import id.andriawan24.cofinance.android.R

val RethinkFontFamily = FontFamily(
    Font(R.font.rethink_sans_bold, FontWeight.Bold),
    Font(R.font.rethink_sans_semibold, FontWeight.SemiBold),
    Font(R.font.rethink_sans_medium, FontWeight.Medium),
    Font(R.font.rethink_sans, FontWeight.Normal),
)

private val defaultTypography = Typography()

val cofinanceTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = RethinkFontFamily,
        fontWeight = FontWeight.Bold
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = RethinkFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = RethinkFontFamily,
        fontWeight = FontWeight.Medium
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = RethinkFontFamily
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = RethinkFontFamily
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = RethinkFontFamily
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = RethinkFontFamily,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = RethinkFontFamily
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = RethinkFontFamily
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = RethinkFontFamily
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = RethinkFontFamily
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = RethinkFontFamily
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = RethinkFontFamily
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = RethinkFontFamily
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = RethinkFontFamily
    )
)