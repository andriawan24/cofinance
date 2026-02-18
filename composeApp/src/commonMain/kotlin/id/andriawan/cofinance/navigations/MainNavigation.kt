package id.andriawan.cofinance.navigations

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.andriawan.cofinance.navigations.destinations.Destinations
import id.andriawan.cofinance.pages.addaccount.AddAccountScreen
import id.andriawan.cofinance.pages.addnew.AddTransactionScreen
import id.andriawan.cofinance.pages.camera.CameraScreen
import id.andriawan.cofinance.pages.login.LoginScreen
import id.andriawan.cofinance.pages.main.MainScreen
import id.andriawan.cofinance.pages.preview.PreviewScreen
import id.andriawan.cofinance.pages.splash.SplashScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Destinations.Splash,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        }
    ) {
        composable<Destinations.Splash> {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Destinations.Main) {
                        launchSingleTop = true
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Destinations.Login) {
                        launchSingleTop = true
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<Destinations.Login> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Destinations.Main) {
                        launchSingleTop = true
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<Destinations.Main> {
            MainScreen(
                parentNavController = navController,
                onNavigateToLogin = {
                    navController.navigate(Destinations.Login) {
                        launchSingleTop = true
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToAdd = {
                    navController.navigate(Destinations.AddNew())
                },
                onNavigateToAddAccount = {
                    navController.navigate(Destinations.AddAccount) {
                        popUpTo<Destinations.Main> {
                            saveState = true
                        }
                        restoreState = true
                    }
                }
            )
        }

        composable<Destinations.AddNew> {
            val route = it.toRoute<Destinations.AddNew>()

            AddTransactionScreen(
                transactionId = route.transactionId,
                onBackPressed = {
                    navController.navigateUp()
                },
                onInputPictureClicked = {
                    navController.navigate(Destinations.Camera)
                },
                onSuccessSave = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("add_activity_result", true)
                    navController.navigateUp()
                }
            )
        }

        composable<Destinations.Camera> {
            CameraScreen(
                onBackPressed = {
                    navController.navigateUp()
                },
                onNavigateToPreview = { imageUri ->
                    println("Image URI $imageUri")
                    navController.navigate(route = Destinations.Preview(imageUrl = imageUri))
                }
            )
        }

        composable<Destinations.Preview> {
            val params = it.toRoute<Destinations.Preview>()

            PreviewScreen(
                imageUrl = params.imageUrl,
                onNavigateToAdd = {
                    navController.navigate(Destinations.AddNew(transactionId = it)) {
                        popUpTo<Destinations.AddNew> {
                            inclusive = true
                        }
                    }
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable<Destinations.AddAccount> {
            AddAccountScreen(
                onBackClicked = {
                    navController.popBackStack()
                },
                onAddAccountSuccess = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("add_account_result", true)
                    navController.popBackStack()
                }
            )
        }
    }
}
