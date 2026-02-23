package id.andriawan.cofinance.pages.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import id.andriawan.cofinance.components.CofinanceBottomNavigation
import id.andriawan.cofinance.models.rememberCofinanceAppState
import id.andriawan.cofinance.navigations.destinations.Destinations
import id.andriawan.cofinance.pages.account.AccountScreen
import id.andriawan.cofinance.pages.account.AccountViewModel
import id.andriawan.cofinance.pages.activity.ActivityScreen
import id.andriawan.cofinance.pages.profile.ProfileScreen
import id.andriawan.cofinance.pages.stats.StatsScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    parentNavController: NavHostController,
    onNavigateToLogin: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditProfile: () -> Unit
) {
    val state = rememberCofinanceAppState()

    Scaffold(
        snackbarHost = { SnackbarHost(state.snackBarHostState) },
        bottomBar = {
            CofinanceBottomNavigation(
                appState = state,
                onNavigateToAdd = onNavigateToAdd
            )
        }
    ) { contentPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            navController = state.navController,
            startDestination = Destinations.Activity,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable<Destinations.Activity> {
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
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(addAccountSucceeded) {
                    if (addAccountSucceeded) {
                        state.showMessage("Successfully add account")

                        accountViewModel.getAccounts()

                        parentNavController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.remove<Boolean>("add_account_result")
                    }
                }

                AccountScreen(onNavigateToAddAccount = onNavigateToAddAccount)
            }

            composable<Destinations.Profile> {
                val resultFlow = parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("edit_profile_result", false)

                val editProfileSucceeded by resultFlow?.collectAsStateWithLifecycle(false)
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(editProfileSucceeded) {
                    if (editProfileSucceeded) {
                        state.showMessage("Profile updated successfully")

                        parentNavController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.remove<Boolean>("edit_profile_result")
                    }
                }

                ProfileScreen(
                    onSignedOut = onNavigateToLogin,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    showMessage = {
                        state.showMessage(it)
                    }
                )
            }
        }
    }
}
