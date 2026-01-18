package id.andriawan.cofinance.navigations.destinations

import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_account
import cofinance.composeapp.generated.resources.ic_activity
import cofinance.composeapp.generated.resources.ic_profile
import cofinance.composeapp.generated.resources.ic_stats
import cofinance.composeapp.generated.resources.label_account
import cofinance.composeapp.generated.resources.label_activity
import cofinance.composeapp.generated.resources.label_profile
import cofinance.composeapp.generated.resources.label_stats
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class BottomNavigationDestinations(
    val icon: DrawableResource,
    val label: StringResource,
    val route: Destinations
) {
    ACTIVITY(
        icon = Res.drawable.ic_activity,
        label = Res.string.label_activity,
        route = Destinations.Activity
    ),
    STATS(
        icon = Res.drawable.ic_stats,
        label = Res.string.label_stats,
        route = Destinations.Stats
    ),
    ACCOUNT(
        icon = Res.drawable.ic_account,
        label = Res.string.label_account,
        route = Destinations.Account
    ),
    PROFILE(
        icon = Res.drawable.ic_profile,
        label = Res.string.label_profile,
        route = Destinations.Profile
    )
}
