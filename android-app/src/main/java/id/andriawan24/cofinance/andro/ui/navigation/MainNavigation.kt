package id.andriawan24.cofinance.andro.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.models.BottomNavigationDestinations
import id.andriawan24.cofinance.andro.ui.presentation.activity.ActivityScreen
import id.andriawan24.cofinance.andro.ui.presentation.addnew.AddNewScreen
import id.andriawan24.cofinance.andro.ui.presentation.camera.CameraScreen
import id.andriawan24.cofinance.andro.ui.presentation.expenses.ExpensesScreen
import id.andriawan24.cofinance.andro.ui.presentation.login.LoginScreen
import id.andriawan24.cofinance.andro.ui.presentation.preview.PreviewScreen
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileScreen
import id.andriawan24.cofinance.andro.ui.presentation.splashscreen.SplashScreen
import id.andriawan24.cofinance.andro.ui.presentation.wallet.WalletScreen
import id.andriawan24.cofinance.andro.utils.CustomNavType
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import kotlin.reflect.typeOf

@Composable
fun MainNavigation(modifier: Modifier = Modifier, appState: CofinanceAppState) {
    NavHost(
        modifier = modifier,
        navController = appState.navController,
        startDestination = Destinations.Splash
    ) {
        composable<Destinations.Splash>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SplashScreen(appState = appState)
        }

        composable<Destinations.Login>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            LoginScreen(appState = appState)
        }

        composable<Destinations.AddNew>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            typeMap = mapOf(typeOf<ReceiptScan>() to CustomNavType.receiptScanType)
        ) {
            val params = it.toRoute<Destinations.AddNew>()
            AddNewScreen(
                appState = appState,
                receiptScan = params.receiptScanned
            )
        }

        composable<Destinations.Camera>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            CameraScreen(appState = appState)
        }

        composable<Destinations.Preview>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val params = it.toRoute<Destinations.Preview>()
            PreviewScreen(appState = appState, imageUri = params.imageUrl.toUri())
        }

        navigation<Destinations.Main>(startDestination = Destinations.Activity) {
            composable<Destinations.Activity>(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                ActivityScreen(
                    onSeeAllTransactionClicked = {
                        appState.navigateToTopLevelDestination(
                            topLevelDestination = BottomNavigationDestinations.ACTIVITY
                        )
                    }
                )
            }

            composable<Destinations.Budget>(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                ExpensesScreen()
            }

            composable<Destinations.Account>(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                WalletScreen()
            }

            composable<Destinations.Profile>(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                ProfileScreen(
                    onSignedOut = {
                        appState.navController.navigate(Destinations.Login) {
                            launchSingleTop = true
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}