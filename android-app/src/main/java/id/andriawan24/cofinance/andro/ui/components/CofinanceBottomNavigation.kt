package id.andriawan24.cofinance.andro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.navigation.models.BottomNavigationDestinations
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions

@Composable
fun CofinanceBottomNavigation(appState: CofinanceAppState) {
    Box {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = Dimensions.zero
        ) {
            appState.bottomNavigationDestinations.forEachIndexed { index, destination ->
                if (index == appState.bottomNavigationDestinations.size / 2) {
                    Spacer(Modifier.weight(1f))
                }

                CofinanceBottomNavigationItem(
                    destination = destination,
                    currentDestination = appState.currentTopBottomNavDest,
                    onItemClicked = {
                        appState.navigateToTopLevelDestination(destination)
                    }
                )
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.TopCenter),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = Dimensions.SIZE_2
            ),
            onClick = {
                appState.navController.navigate(Destinations.AddExpenses)
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        }
    }
}

@Composable
fun RowScope.CofinanceBottomNavigationItem(
    destination: BottomNavigationDestinations,
    currentDestination: BottomNavigationDestinations?,
    onItemClicked: () -> Unit
) {
    NavigationBarItem(
        selected = currentDestination == destination,
        label = {
            Text(
                text = stringResource(destination.iconTextId),
                style = MaterialTheme.typography.labelSmall
            )
        },
        alwaysShowLabel = true,
        icon = {
            if (currentDestination == destination) {
                Icon(
                    painter = painterResource(destination.selectedIcon),
                    contentDescription = null
                )
            } else {
                Icon(
                    painter = painterResource(destination.unselectedIcon),
                    contentDescription = null
                )
            }
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f),
            selectedTextColor = MaterialTheme.colorScheme.onBackground,
            unselectedTextColor = MaterialTheme.colorScheme.onBackground,
            selectedIconColor = MaterialTheme.colorScheme.onBackground,
            unselectedIconColor = MaterialTheme.colorScheme.onBackground
        ),
        onClick = onItemClicked
    )
}

@Preview(showBackground = true)
@Composable
private fun CofinanceBottomNavigationItemPreview() {
    CofinanceTheme {
        Surface {
            Row {
                CofinanceBottomNavigationItem(
                    destination = BottomNavigationDestinations.HOME,
                    currentDestination = BottomNavigationDestinations.HOME,
                    onItemClicked = {}
                )

                CofinanceBottomNavigationItem(
                    destination = BottomNavigationDestinations.EXPENSES,
                    currentDestination = BottomNavigationDestinations.HOME,
                    onItemClicked = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun CofinanceBottomNavigationPreview() {
    CofinanceTheme {
        CofinanceBottomNavigation(appState = rememberCofinanceAppState())
    }
}