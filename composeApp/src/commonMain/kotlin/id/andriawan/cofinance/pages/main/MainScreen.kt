package id.andriawan.cofinance.pages.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import id.andriawan.cofinance.models.CofinanceAppState
import id.andriawan.cofinance.navigations.Destinations

@Composable
fun MainScreen(
    appState: CofinanceAppState,
    onNavigateToLogin: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAddAccount: () -> Unit
) {
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
                val addAccountSucceeded by appState.parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("add_activity_result", false)
                    ?.collectAsStateWithLifecycle(false)
                    ?: remember {
                        mutableStateOf(false)
                    }

//                LaunchedEffect(addAccountSucceeded) {
//                    if (addAccountSucceeded) {
//                        appState.showSnackbar("Successfully add activity")
//                        activityViewModel.fetchTransaction()
//                        parentNavController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>(
//                            "add_activity_result"
//                        )
//                    }
//                }

                ActivityScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Stats> {
                StatsScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Account> {
                // val accountViewModel: AccountViewModel = koinViewModel()

//                val resultFlow = parentNavController.currentBackStackEntry
//                    ?.savedStateHandle
//                    ?.getStateFlow("add_account_result", false)
//
//                val addAccountSucceeded by resultFlow?.collectAsStateWithLifecycle(false)
//                    ?: remember {
//                        mutableStateOf(false)
//                    }
//
//                LaunchedEffect(addAccountSucceeded) {
//                    if (addAccountSucceeded) {
//                        appState.showSnackbar("Successfully add account")
//                        accountViewModel.getAccounts()
//                        parentNavController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>(
//                            "add_account_result"
//                        )
//                    }
//                }

                AccountScreen(onNavigateToAddAccount = onNavigateToAddAccount)
            }

            composable<Destinations.Profile> {
                ProfileScreen(
                    onSignedOut = onNavigateToLogin,
                    showMessage = {
                        appState.showMessage(it)
                    }
                )
            }
        }
    }
}
