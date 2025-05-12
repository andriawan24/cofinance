package id.andriawan24.cofinance.andro.ui.models

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import id.andriawan24.cofinance.andro.ui.navigation.models.BottomNavigationDestinations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberCofinanceAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController()
): CofinanceAppState {
    return remember(coroutineScope, navController) {
        CofinanceAppState(navController = navController, coroutineScope = coroutineScope)
    }
}

@Stable
class CofinanceAppState(val navController: NavHostController, val coroutineScope: CoroutineScope) {
    val bottomNavigationDestinations = BottomNavigationDestinations.entries
    val bottomNavigationRoutes = bottomNavigationDestinations.map { it.routeClass.route }
    var snackBarHostState = SnackbarHostState()

    private val previousDestination = mutableStateOf<NavDestination?>(null)
    val currentDestination: NavDestination?
        @Composable get() {
            val currentEntry =
                navController.currentBackStackEntryFlow.collectAsState(initial = null)
            return currentEntry.value?.destination.also { destination ->
                if (destination != null) {
                    previousDestination.value = destination
                }
            } ?: previousDestination.value
        }

    val currentTopBottomNavDest: BottomNavigationDestinations?
        @Composable get() {
            return bottomNavigationDestinations.firstOrNull { topLevelDestination ->
                currentDestination?.hasRoute(route = topLevelDestination.route) == true
            }
        }

    fun navigateToTopLevelDestination(topLevelDestination: BottomNavigationDestinations) {
        val topLevelOption = navOptions {
            popUpTo(BottomNavigationDestinations.entries.first().routeClass.route) {
                saveState = true
            }
            restoreState = true
        }

        navController.navigate(route = topLevelDestination.routeClass, navOptions = topLevelOption)
    }

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackBarHostState.showSnackbar(message)
        }
    }
}