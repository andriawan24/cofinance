package id.andriawan24.cofinance.android.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import id.andriawan24.cofinance.android.ui.models.CofinanceAppState
import id.andriawan24.cofinance.android.ui.navigation.models.BottomNavigationDestinations
import id.andriawan24.cofinance.android.ui.presentation.expenses.ExpensesScreen
import id.andriawan24.cofinance.android.ui.presentation.home.HomeScreen
import id.andriawan24.cofinance.android.ui.presentation.login.LoginScreen
import id.andriawan24.cofinance.android.ui.presentation.onboarding.OnboardingScreen
import id.andriawan24.cofinance.android.ui.presentation.profile.ProfileScreen
import id.andriawan24.cofinance.android.ui.presentation.wallet.WalletScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier, appState: CofinanceAppState) {
    NavHost(
        modifier = modifier,
        navController = appState.navController,
        startDestination = Destinations.Onboarding
    ) {
        composable<Destinations.Onboarding>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            OnboardingScreen(
                onContinueClicked = {
                    appState.navController.navigate(route = Destinations.Login)
                }
            )
        }

        composable<Destinations.Login>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            LoginScreen(
                onSignedIn = {
                    appState.navController.navigate(Destinations.Home) {
                        launchSingleTop = true
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        navigation<Destinations.Main>(startDestination = Destinations.Home) {
            composable<Destinations.Home>(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                HomeScreen(
                    onSeeAllTransactionClicked = {
                        appState.navigateToTopLevelDestination(
                            topLevelDestination = BottomNavigationDestinations.EXPENSES
                        )
                    }
                )
            }

            composable<Destinations.Expenses>(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                ExpensesScreen()
            }

            composable<Destinations.Wallet>(
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
                ProfileScreen()
            }
        }
    }
}