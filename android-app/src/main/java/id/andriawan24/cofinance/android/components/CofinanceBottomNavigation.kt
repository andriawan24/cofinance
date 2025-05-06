package id.andriawan24.cofinance.android.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.android.models.CofinanceAppState
import id.andriawan24.cofinance.android.models.rememberCofinanceAppState
import id.andriawan24.cofinance.android.navigation.models.BottomNavigationDestinations
import id.andriawan24.cofinance.android.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.Dimensions

@Composable
fun CofinanceBottomNavigation(appState: CofinanceAppState) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = Dimensions.zero
    ) {
        appState.bottomNavigationDestinations.forEach { destination ->
            CofinanceBottomNavigationItem(
                destination = destination,
                currentDestination = appState.currentTopBottomNavDest,
                onItemClicked = {
                    appState.navigateToTopLevelDestination(destination)
                }
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