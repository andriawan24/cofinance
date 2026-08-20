package id.andriawan.cofinance.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import id.andriawan.cofinance.data.lock.AutoLockController
import id.andriawan.cofinance.di.databaseModule
import id.andriawan.cofinance.di.networkModule
import id.andriawan.cofinance.di.repositoryModule
import id.andriawan.cofinance.di.securityModule
import id.andriawan.cofinance.di.useCaseModule
import id.andriawan.cofinance.di.viewModelModule
import id.andriawan.cofinance.localization.AppLang
import id.andriawan.cofinance.localization.rememberAppLocale
import id.andriawan.cofinance.navigations.MainNavigation
import id.andriawan.cofinance.theme.CofinanceTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import coil3.compose.LocalPlatformContext

val LocalAppLocalization = compositionLocalOf { AppLang.English }

@Composable
@Preview
fun App(sharedImageUri: String? = null) {
    val platformContext = LocalPlatformContext.current
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(
                    networkModule,
                    databaseModule(platformContext),
                    repositoryModule,
                    securityModule,
                    useCaseModule,
                    viewModelModule
                )
            }
        ),
        content = {
            val currentLanguage = rememberAppLocale()

            AutoLockLifecycle()

            CompositionLocalProvider(LocalAppLocalization provides currentLanguage) {
                CofinanceTheme {
                    MainNavigation(sharedImageUri = sharedImageUri)
                }
            }
        }
    )
}

/**
 * Hands the app's foreground and background transitions to [AutoLockController].
 *
 * Without this the controller exists and does nothing: the timeout never starts, so the data key
 * stays in memory for as long as the process lives and the one-minute auto-lock never fires. It is
 * placed at the root of the composition rather than on a screen because the lock is a property of
 * held key material and must not depend on which screen the user happened to leave from.
 *
 * `ON_STOP` and `ON_START` rather than pause and resume: a permission dialog or a system sheet
 * pauses the activity without the app leaving the foreground, and locking on those would make the
 * lock fire during the app's own flows.
 */
@Composable
private fun AutoLockLifecycle() {
    val autoLock = koinInject<AutoLockController>()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { autoLock.onEnterBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { autoLock.onEnterForeground() }
}
