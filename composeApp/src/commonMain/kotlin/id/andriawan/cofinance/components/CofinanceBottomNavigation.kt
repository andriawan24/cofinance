package id.andriawan.cofinance.components

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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_add
import id.andriawan.cofinance.models.CofinanceAppState
import id.andriawan.cofinance.models.rememberCofinanceAppState
import id.andriawan.cofinance.navigations.destinations.BottomNavigationDestinations
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CofinanceBottomNavigation(
    appState: CofinanceAppState,
    onNavigateToAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .dropShadow(
                shape = RectangleShape,
                shadow = Shadow(
                    radius = Dimensions.SIZE_10,
                    spread = Dimensions.SIZE_2,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    offset = DpOffset(x = Dimensions.zero, y = -Dimensions.SIZE_4)
                )
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
                    shadow = Shadow(
                        radius = Dimensions.SIZE_16,
                        spread = Dimensions.SIZE_2,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        offset = DpOffset(x = Dimensions.SIZE_4, y = Dimensions.SIZE_4)
                    )
                )
                .align(Alignment.TopCenter),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Dimensions.zero),
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToAdd
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_add),
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
                text = stringResource(destination.label),
                style = MaterialTheme.typography.labelSmall
            )
        },
        alwaysShowLabel = true,
        icon = {
            Icon(
                painter = painterResource(destination.icon),
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
                    destination = BottomNavigationDestinations.STATS,
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
        CofinanceBottomNavigation(
            appState = rememberCofinanceAppState(),
            onNavigateToAdd = {}
        )
    }
}
