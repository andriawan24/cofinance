package id.andriawan.cofinance.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan.cofinance.di.databaseModule
import id.andriawan.cofinance.di.networkModule
import id.andriawan.cofinance.di.repositoryModule
import id.andriawan.cofinance.di.useCaseModule
import id.andriawan.cofinance.di.viewModelModule
import id.andriawan.cofinance.localization.AppLang
import id.andriawan.cofinance.localization.rememberAppLocale
import id.andriawan.cofinance.navigations.MainNavigation
import id.andriawan.cofinance.theme.CofinanceTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

val LocalAppLocalization = compositionLocalOf { AppLang.English }

@Composable
@Preview
fun App(sharedImageUri: String? = null) {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(
                    networkModule,
                    databaseModule,
                    repositoryModule,
                    useCaseModule,
                    viewModelModule
                )
            }
        ),
        content = {
            val currentLanguage = rememberAppLocale()

            CompositionLocalProvider(LocalAppLocalization provides currentLanguage) {
                CofinanceTheme {
                    MainNavigation(sharedImageUri = sharedImageUri)
                }
            }
        }
    )
}
