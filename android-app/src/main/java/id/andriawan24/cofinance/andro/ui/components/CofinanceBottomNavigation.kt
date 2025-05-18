package id.andriawan24.cofinance.andro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.navigation.models.BottomNavigationDestinations
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.dropShadow

@Composable
fun CofinanceBottomNavigation(appState: CofinanceAppState) {
    Box(
        modifier = Modifier
            .dropShadow(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RectangleShape,
                offsetY = -Dimensions.SIZE_4,
                blur = Dimensions.SIZE_10
            )
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(top = Dimensions.SIZE_16)
    ) {
        NavigationBar(
            modifier = Modifier,
            containerColor = MaterialTheme.colorScheme.onPrimary,
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
            modifier = Modifier
                .dropShadow(
                    shape = CircleShape,
                    blur = Dimensions.SIZE_16,
                    offsetY = Dimensions.SIZE_4,
                    offsetX = Dimensions.SIZE_4,
                    spread = Dimensions.SIZE_10,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                .align(Alignment.TopCenter),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Dimensions.zero),
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = { appState.navController.navigate(Destinations.AddNew) }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
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
                text = stringResource(destination.labelId),
                style = MaterialTheme.typography.labelSmall
            )
        },
        alwaysShowLabel = true,
        icon = {
            Icon(
                painter = painterResource(destination.iconId),
                contentDescription = null
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = Color.Unspecified
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
                    destination = BottomNavigationDestinations.ACTIVITY,
                    currentDestination = BottomNavigationDestinations.ACTIVITY,
                    onItemClicked = {}
                )

                CofinanceBottomNavigationItem(
                    destination = BottomNavigationDestinations.BUDGET,
                    currentDestination = BottomNavigationDestinations.ACTIVITY,
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