package id.andriawan.cofinance.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import id.andriawan.cofinance.di.networkModule
import id.andriawan.cofinance.di.repositoryModule
import id.andriawan.cofinance.di.useCaseModule
import id.andriawan.cofinance.di.viewModelModule
import id.andriawan.cofinance.localization.AppLang
import id.andriawan.cofinance.localization.rememberAppLocale
import id.andriawan.cofinance.navigations.MainNavigation
import id.andriawan.cofinance.theme.CofinanceTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication

val LocalAppLocalization = compositionLocalOf { AppLang.English }

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(
            networkModule,
            repositoryModule,
            useCaseModule,
            viewModelModule
        )
    }) {
        val currentLanguage = rememberAppLocale()

        CompositionLocalProvider(LocalAppLocalization provides currentLanguage) {
            CofinanceTheme {
                MainNavigation()
            }
        }
    }
}
