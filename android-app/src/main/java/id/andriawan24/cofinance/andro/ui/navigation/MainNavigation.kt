package id.andriawan24.cofinance.andro.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.andriawan24.cofinance.andro.ui.presentation.addnew.AddTransactionScreen
import id.andriawan24.cofinance.andro.ui.presentation.camera.CameraScreen
import id.andriawan24.cofinance.andro.ui.presentation.login.LoginScreen
import id.andriawan24.cofinance.andro.ui.presentation.main.MainScreen
import id.andriawan24.cofinance.andro.ui.presentation.preview.PreviewScreen
import id.andriawan24.cofinance.andro.ui.presentation.splashscreen.SplashScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier, navController: NavHostController) {
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

        composable<Destinations.AddNew> {
            AddTransactionScreen(
                onBackPressed = {
                    navController.navigateUp()
                },
                onInputPictureClicked = {
                    navController.navigate(Destinations.Camera)
                },
                onSuccessSave = {
                    // TODO: Just do this so far
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
                    navController.navigate(route = Destinations.Preview(imageUrl = imageUri.toString()))
                }
            )
        }

        composable<Destinations.Preview> {
            val params = it.toRoute<Destinations.Preview>()

            PreviewScreen(
                imageUri = params.imageUrl.toUri(),
                onNavigateToAdd = {
                    navController.navigate(Destinations.AddNew) {
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

        composable<Destinations.Main> {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(Destinations.Login) {
                        launchSingleTop = true
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToAdd = {
                    navController.navigate(Destinations.AddNew)
                }
            )
        }
    }
}