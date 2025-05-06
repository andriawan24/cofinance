package id.andriawan24.cofinance.android.navigation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.painter.Painter
import id.andriawan24.cofinance.android.R
import id.andriawan24.cofinance.android.navigation.Destinations
import kotlin.reflect.KClass

enum class BottomNavigationDestinations(
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int,
    @StringRes val iconTextId: Int,
    val route: KClass<*>,
    val routeClass: Destinations
) {
    HOME(
        selectedIcon = R.drawable.ic_home_filled,
        unselectedIcon = R.drawable.ic_home_outlined,
        iconTextId = R.string.label_home,
        route = Destinations.Home::class,
        routeClass = Destinations.Home
    ),
    EXPENSES(
        selectedIcon = R.drawable.ic_expenses_filled,
        unselectedIcon = R.drawable.ic_expenses_outlined,
        iconTextId = R.string.label_expenses,
        route = Destinations.Expenses::class,
        routeClass = Destinations.Expenses
    ),
    WALLET(
        selectedIcon = R.drawable.ic_wallet_filled,
        unselectedIcon = R.drawable.ic_wallet_outlined,
        iconTextId = R.string.label_wallet,
        route = Destinations.Wallet::class,
        routeClass = Destinations.Wallet
    ),
    PROFILE(
        selectedIcon = R.drawable.ic_person_filled,
        unselectedIcon = R.drawable.ic_person_outlined,
        iconTextId = R.string.label_profile,
        route = Destinations.Profile::class,
        routeClass = Destinations.Profile
    )
}