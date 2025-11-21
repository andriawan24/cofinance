package id.andriawan24.cofinance.andro.ui.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.CofinanceBottomNavigation
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.presentation.account.AccountScreen
import id.andriawan24.cofinance.andro.ui.presentation.account.AccountViewModel
import id.andriawan24.cofinance.andro.ui.presentation.activity.ActivityScreen
import id.andriawan24.cofinance.andro.ui.presentation.activity.ActivityViewModel
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileScreen
import id.andriawan24.cofinance.andro.ui.presentation.stats.StatsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    parentNavController: NavHostController,
    onNavigateToLogin: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAddAccount: () -> Unit
) {
    val appState = rememberCofinanceAppState()
    val context = LocalContext.current

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
                val activityViewModel: ActivityViewModel = koinViewModel()

                val addAccountSucceeded by parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("add_activity_result", false)
                    ?.collectAsStateWithLifecycle(false)
                    ?: remember {
                        mutableStateOf(false)
                    }

                LaunchedEffect(addAccountSucceeded) {
                    if (addAccountSucceeded) {
                        appState.showSnackbar("Successfully add activity")
                        activityViewModel.fetchTransaction()
                        parentNavController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>(
                            "add_activity_result"
                        )
                    }
                }

                ActivityScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Stats> {
                StatsScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Account> {
                val accountViewModel: AccountViewModel = koinViewModel()

                val resultFlow = parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("add_account_result", false)

                val addAccountSucceeded by resultFlow?.collectAsStateWithLifecycle(false)
                    ?: remember {
                        mutableStateOf(false)
                    }

                LaunchedEffect(addAccountSucceeded) {
                    if (addAccountSucceeded) {
                        appState.showSnackbar("Successfully add account")
                        accountViewModel.getAccounts()
                        parentNavController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>(
                            "add_account_result"
                        )
                    }
                }

                AccountScreen(onNavigateToAddAccount = onNavigateToAddAccount)
            }

            composable<Destinations.Profile> {
                val profileUpdatedFlow = parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("profile_updated", false)

                val profileUpdated by profileUpdatedFlow?.collectAsStateWithLifecycle(false)
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(profileUpdated) {
                    if (profileUpdated) {
                        appState.showSnackbar(context.getString(R.string.message_profile_updated))
                    }
                }

                ProfileScreen(
                    onSignedOut = onNavigateToLogin,
                    showMessage = { appState.showSnackbar(it) },
                    onEditProfile = {
                        parentNavController.navigate(Destinations.EditProfile)
                    },
                    shouldRefreshProfile = profileUpdated,
                    onProfileRefreshed = {
                        parentNavController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.remove<Boolean>("profile_updated")
                    }
                )
            }
        }
    }
}
