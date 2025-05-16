package id.andriawan24.cofinance.andro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C89FE),
    onPrimary = Color.White, // Typical text/icon color on primary
    primaryContainer = Color(0xFFD0E4FF), // Lighter shade of primary
    onPrimaryContainer = Color(0xFF001E30), // Darker text color on primary container
    secondary = Color(0xFF05603A),
    onSecondary = Color.White, // Typical text/icon color on secondary
    secondaryContainer = Color(0xFFB8EAC9), // Lighter shade of secondary
    onSecondaryContainer = Color(0xFF012112), // Darker text color on secondary container
    tertiary = Color(0xFF3700B3),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0BCFF),
    onTertiaryContainer = Color(0xFF2E114F),
    background = Color(0xFFFBFCFE),
    onBackground = Color(0xFF1A1C1E),
    surfaceTint = Color(0xFF475D92),
    surface = Color(0xFFEEF9F8),
    onSurface = Color.White,
    surfaceVariant = Color(0xFFF0F3FF),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFFADD7FF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outlineVariant = Color(0xFFC3C6CF),
    scrim = Color(0xFF000000),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0766EB), // Use the same primary for consistency
    onPrimary = Color.White, // Typical text/icon color on primary
    primaryContainer = Color(0xFF2F4578), // Darker shade of primary
    onPrimaryContainer = Color(0xFFD0E4FF), // Lighter text on dark primary container
    secondary = Color(0xFF05603A), // Use the same secondary for consistency
    onSecondary = Color.White, // Typical text/icon color on secondary
    secondaryContainer = Color(0xFF004428), // Darker shade of secondary
    onSecondaryContainer = Color(0xFFB8EAC9), // Lighter text on dark secondary container
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF49259D),
    tertiaryContainer = Color(0xFF5E3FC8),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF060E22),
    surfaceTint = Color(0xFFB1C6FF),
    onSurface = Color(0xFF162E60),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    inverseOnSurface = Color(0xFF2F3033),
    inverseSurface = Color(0xFFE2E2E5),
    inversePrimary = Color(0xFF005F99),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
)