package id.andriawan24.cofinance.andro.ui.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import id.andriawan24.cofinance.andro.ui.components.CofinanceBottomNavigation
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.presentation.account.AccountScreen
import id.andriawan24.cofinance.andro.ui.presentation.activity.ActivityScreen
import id.andriawan24.cofinance.andro.ui.presentation.expenses.ExpensesScreen
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileScreen

@Composable
fun MainScreen(onNavigateToLogin: () -> Unit, onNavigateToAdd: () -> Unit) {
    val appState = rememberCofinanceAppState()

    Scaffold(
        bottomBar = {
            CofinanceBottomNavigation(
                appState = appState,
                onNavigateToAdd = onNavigateToAdd
            )
        },
        snackbarHost = { SnackbarHost(appState.snackBarHostState) }
    ) { contentPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            navController = appState.navController,
            startDestination = Destinations.Activity
        ) {
            composable<Destinations.Activity> {
                ActivityScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Budget> {
                ExpensesScreen()
            }

            composable<Destinations.Account> {
                AccountScreen()
            }

            composable<Destinations.Profile> {
                ProfileScreen(onSignedOut = onNavigateToLogin)
            }
        }
    }
}
