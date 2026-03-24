package id.andriawan.cofinance.pages.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import id.andriawan.cofinance.components.CofinanceBottomNavigation
import id.andriawan.cofinance.models.rememberCofinanceAppState
import id.andriawan.cofinance.navigations.destinations.Destinations
import id.andriawan.cofinance.pages.account.AccountScreen
import id.andriawan.cofinance.pages.activity.ActivityScreen
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.message_account_added
import cofinance.composeapp.generated.resources.message_profile_updated
import id.andriawan.cofinance.pages.profile.ProfileScreen
import id.andriawan.cofinance.pages.profile.ProfileViewModel
import id.andriawan.cofinance.pages.stats.StatsScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    parentNavController: NavHostController,
    onNavigateToLogin: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToCycleReview: () -> Unit = {},
    onNavigateToEditTransaction: (String) -> Unit = {}
) {
    val state = rememberCofinanceAppState()
    val accountAddedMessage = stringResource(Res.string.message_account_added)
    val profileUpdatedMessage = stringResource(Res.string.message_profile_updated)

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
                ActivityScreen(
                    onNavigateToAdd = onNavigateToAdd,
                    onNavigateToEditTransaction = onNavigateToEditTransaction,
                    onNavigateToCycleReview = onNavigateToCycleReview
                )
            }

            composable<Destinations.Stats> {
                StatsScreen(onNavigateToAdd = onNavigateToAdd)
            }

            composable<Destinations.Account> {
                val resultFlow = parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("add_account_result", false)

                val addAccountSucceeded by resultFlow?.collectAsStateWithLifecycle(false)
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(addAccountSucceeded) {
                    if (addAccountSucceeded) {
                        state.showMessage(accountAddedMessage)

                        // No need to manually call getAccounts() — the PowerSync
                        // watch flow automatically re-emits on local DB changes.

                        parentNavController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.remove<Boolean>("add_account_result")
                    }
                }

                AccountScreen(onNavigateToAddAccount = onNavigateToAddAccount)
            }

            composable<Destinations.Profile> {
                val profileViewModel = koinViewModel<ProfileViewModel>()

                val resultFlow = parentNavController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("edit_profile_result", false)

                val editProfileSucceeded by resultFlow?.collectAsStateWithLifecycle(false)
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(editProfileSucceeded) {
                    if (editProfileSucceeded) {
                        profileViewModel.refreshUser()
                        state.showMessage(profileUpdatedMessage)

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
                    },
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}
