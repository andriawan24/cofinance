package id.andriawan.cofinance.navigations

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.andriawan.cofinance.data.ocr.parser.decodeReceiptFields
import id.andriawan.cofinance.navigations.destinations.Destinations
import id.andriawan.cofinance.pages.addaccount.AddAccountScreen
import id.andriawan.cofinance.pages.addnew.AddTransactionScreen
import id.andriawan.cofinance.pages.camera.CameraScreen
import id.andriawan.cofinance.pages.cyclereview.CycleReviewScreen
import id.andriawan.cofinance.pages.editprofile.EditProfileScreen
import id.andriawan.cofinance.pages.encryption.EncryptionSetupScreen
import id.andriawan.cofinance.pages.encryption.RecoveryPhraseRestoreScreen
import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.pages.lock.UnlockScreen
import id.andriawan.cofinance.pages.login.LoginScreen
import id.andriawan.cofinance.pages.main.MainScreen
import id.andriawan.cofinance.pages.preview.PreviewScreen
import id.andriawan.cofinance.pages.splash.LaunchRoute
import id.andriawan.cofinance.pages.splash.SplashScreen
import id.andriawan.cofinance.data.session.SessionPolicy
import org.koin.compose.koinInject

@Composable
fun MainNavigation(modifier: Modifier = Modifier, sharedImageUri: String? = null) {
    val navController = rememberNavController()
    val sessionPolicy = koinInject<SessionPolicy>()
    val encryptionSession = koinInject<EncryptionSession>()

    fun navigateToMain() {
        navController.navigate(Destinations.Main) {
            launchSingleTop = true
            popUpTo(0) { inclusive = true }
        }
    }

    /**
     * Restarts the launch sequence, which is the only place that decides between setup, unlock,
     * migration, and the main experience.
     *
     * Every edge that produces an unlocked session — signing in, finishing setup, finishing a
     * restore, and auto-lock releasing — comes back through here rather than jumping to main, so
     * the decision exists once.
     */
    fun navigateToLaunch() {
        navController.navigate(Destinations.Splash) {
            launchSingleTop = true
            popUpTo(0) { inclusive = true }
        }
    }

    // Auto-lock clears the data key while the app is in the background, and the screen the user
    // returns to must not keep showing finance data decrypted under it. The launch sequence is what
    // presents the unlock, so a session that goes locked while anywhere else sends the app back to
    // it. Splash, setup, and restore are excluded because they are the states that resolve a locked
    // or absent session in the first place.
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val sessionState by encryptionSession.state.collectAsStateWithLifecycle()
    LaunchedEffect(sessionState, currentDestination) {
        val isLaunchDestination = currentDestination == null ||
            currentDestination.hasRoute(Destinations.Splash::class) ||
            currentDestination.hasRoute(Destinations.EncryptionSetup::class) ||
            currentDestination.hasRoute(Destinations.RecoveryPhraseRestore::class) ||
            currentDestination.hasRoute(Destinations.Unlock::class) ||
            currentDestination.hasRoute(Destinations.Login::class)

        if (sessionState == EncryptionSessionState.Locked && !isLaunchDestination) {
            navigateToLaunch()
        }
    }

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
                onNavigate = { route ->
                    when (route) {
                        LaunchRoute.Main -> {
                            navigateToMain()
                            // If opened via share intent, navigate to Preview immediately
                            if (sharedImageUri != null && sessionPolicy.isSignedIn()) {
                                navController.navigate(
                                    Destinations.Preview(imageUrl = sharedImageUri)
                                )
                            } else if (sharedImageUri != null) {
                                navController.navigate(Destinations.Login)
                            }
                        }

                        LaunchRoute.EncryptionSetup -> navController.navigate(
                            Destinations.EncryptionSetup
                        ) {
                            launchSingleTop = true
                            popUpTo(0) { inclusive = true }
                        }

                        LaunchRoute.Unlock -> navController.navigate(Destinations.Unlock) {
                            launchSingleTop = true
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<Destinations.Login> {
            LoginScreen(
                onNavigateToHome = {
                    // Back through the launch sequence rather than straight to main. Encryption
                    // setup is gated at sign-in — the phrase protects the copy that leaves the
                    // device, and a user who never signs in has no such copy — and the same
                    // sequence also converts any plaintext this account still has in the cloud.
                    // Deciding that here as well would be a second copy of the rule.
                    navigateToLaunch()
                }
            )
        }

        composable<Destinations.EncryptionSetup> {
            EncryptionSetupScreen(
                // Through the launch sequence: a user who just completed setup is exactly the user
                // whose cloud records may still be plaintext, and migration needs the data key
                // setup has only now produced.
                onSetupComplete = { navigateToLaunch() },
                onRestoreRequired = {
                    navController.navigate(Destinations.RecoveryPhraseRestore) {
                        launchSingleTop = true
                        popUpTo<Destinations.EncryptionSetup> { inclusive = true }
                    }
                }
            )
        }

        composable<Destinations.Unlock> {
            UnlockScreen(
                // Back to the launch sequence rather than straight to main: a session that has just
                // been unlocked is one migration may now have a data key for.
                onUnlocked = { navigateToLaunch() },
                onRecoveryPhraseRequired = {
                    navController.navigate(Destinations.RecoveryPhraseRestore) {
                        launchSingleTop = true
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Destinations.RecoveryPhraseRestore> {
            RecoveryPhraseRestoreScreen(onRestored = { navigateToLaunch() })
        }

        composable<Destinations.Main> {
            MainScreen(
                parentNavController = navController,
                onNavigateToLogin = {
                    navController.navigate(Destinations.Login) {
                        launchSingleTop = true
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
                },
                onNavigateToEditProfile = {
                    navController.navigate(Destinations.EditProfile) {
                        popUpTo<Destinations.Main> {
                            saveState = true
                        }
                        restoreState = true
                    }
                },
                onNavigateToCycleReview = {
                    navController.navigate(Destinations.CycleReview)
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Destinations.AddNew(transactionId = transactionId))
                }
            )
        }

        composable<Destinations.AddNew> {
            val route = it.toRoute<Destinations.AddNew>()

            AddTransactionScreen(
                transactionId = route.transactionId,
                lowConfidenceFields = decodeReceiptFields(route.lowConfidenceFields),
                onBackPressed = {
                    navController.navigateUp()
                },
                onInputPictureClicked = {
                    if (sessionPolicy.isSignedIn()) {
                        navController.navigate(Destinations.Camera)
                    } else {
                        navController.navigate(Destinations.Login) { launchSingleTop = true }
                    }
                },
                onSuccessSave = {
                    val previousEntry = navController.previousBackStackEntry
                    if (previousEntry?.destination?.hasRoute(Destinations.Preview::class) == true) {
                        navController.popBackStack<Destinations.Main>(inclusive = false)
                    } else {
                        previousEntry?.savedStateHandle?.set("add_activity_result", true)
                        navController.navigateUp()
                    }
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
                onNavigateToAdd = { transactionId, lowConfidenceFields ->
                    navController.navigate(
                        Destinations.AddNew(
                            transactionId = transactionId,
                            lowConfidenceFields = lowConfidenceFields
                        )
                    ) {
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

        composable<Destinations.CycleReview> {
            CycleReviewScreen(
                onCompleted = {
                    navController.popBackStack()
                }
            )
        }

        composable<Destinations.EditProfile> {
            EditProfileScreen(
                onBackClicked = {
                    navController.popBackStack()
                },
                onProfileUpdated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("edit_profile_result", true)
                    navController.popBackStack()
                }
            )
        }
    }
}
