package id.andriawan.cofinance.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.compose.rememberNavController
import id.andriawan.cofinance.localization.AppLang
import id.andriawan.cofinance.localization.rememberAppLocale
import id.andriawan.cofinance.navigations.MainNavigation
import id.andriawan.cofinance.theme.CofinanceTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

val LocalAppLocalization = compositionLocalOf { AppLang.English }

@Composable
@Preview
fun App() {
    val currentLanguage = rememberAppLocale()

    CompositionLocalProvider(LocalAppLocalization provides currentLanguage) {
        CofinanceTheme {
            val navController = rememberNavController()
            MainNavigation(navController = navController)
        }
    }
}