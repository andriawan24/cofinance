package id.andriawan24.cofinance.andro.ui.navigation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.navigation.Destinations

enum class BottomNavigationDestinations(
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
    val route: Destinations
) {
    ACTIVITY(
        iconId = R.drawable.ic_activity,
        labelId = R.string.label_activity,
        route = Destinations.Activity
    ),
    BUDGET(
        iconId = R.drawable.ic_budget,
        labelId = R.string.label_budget,
        route = Destinations.Budget
    ),
    ACCOUNT(
        iconId = R.drawable.ic_account,
        labelId = R.string.label_account,
        route = Destinations.Account
    ),
    PROFILE(
        iconId = R.drawable.ic_profile,
        labelId = R.string.label_profile,
        route = Destinations.Profile
    )
}