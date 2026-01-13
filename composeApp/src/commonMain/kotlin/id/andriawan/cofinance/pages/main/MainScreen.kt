package id.andriawan.cofinance.pages.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.andriawan.cofinance.components.CofinanceBottomNavigation
import id.andriawan.cofinance.models.CofinanceAppState
import id.andriawan.cofinance.models.rememberCofinanceAppState
import id.andriawan.cofinance.navigations.Destinations
import id.andriawan.cofinance.pages.activity.ActivityScreen

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAddAccount: () -> Unit
) {
    val state = rememberCofinanceAppState()

    Scaffold(
        bottomBar = {
            CofinanceBottomNavigation(
                appState = state,
                onNavigateToAdd = onNavigateToAdd
            )
        },
        snackbarHost = { SnackbarHost(state.snackBarHostState) }
    ) { contentPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            navController = state.navController,
            startDestination = Destinations.Activity
        ) {
            composable<Destinations.Activity> {
                ActivityScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Stats> {
                // StatsScreen(onNavigateToAdd = onNavigateToAdd)
                Text("Hello World")
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

                // AccountScreen(onNavigateToAddAccount = onNavigateToAddAccount)
                Text("Hello World")

            }

            composable<Destinations.Profile> {
//                ProfileScreen(
//                    onSignedOut = onNavigateToLogin,
//                    showMessage = {
//                        appState.showMessage(it)
//                    }
//                )
                Text("Hello World")

            }
        }
    }
}
