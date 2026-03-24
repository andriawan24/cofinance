package id.andriawan.cofinance.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.data.local.rememberCofinanceDatabase
import id.andriawan.cofinance.di.databaseModule
import id.andriawan.cofinance.di.networkModule
import id.andriawan.cofinance.di.repositoryModule
import id.andriawan.cofinance.di.useCaseModule
import id.andriawan.cofinance.di.viewModelModule
import kotlinx.coroutines.launch
import id.andriawan.cofinance.localization.AppLang
import id.andriawan.cofinance.localization.rememberAppLocale
import id.andriawan.cofinance.navigations.MainNavigation
import id.andriawan.cofinance.theme.CofinanceTheme
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication

val LocalAppLocalization = compositionLocalOf { AppLang.English }

@Composable
@Preview
fun App(sharedImageUri: String? = null) {
    val database = rememberCofinanceDatabase()

    KoinApplication(application = {
        modules(
            networkModule,
            databaseModule(database),
            repositoryModule,
            useCaseModule,
            viewModelModule
        )
    }) {
        val scope = rememberCoroutineScope()

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            scope.launch { database.pauseSync() }
        }

        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            scope.launch { database.resumeSync() }
        }

        val currentLanguage = rememberAppLocale()

        CompositionLocalProvider(LocalAppLocalization provides currentLanguage) {
            CofinanceTheme {
                MainNavigation(sharedImageUri = sharedImageUri)
            }
        }
    }
}
