package id.andriawan24.cofinance.andro.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destinations(val route: String) {
    @Serializable
    data object Onboarding : Destinations(Onboarding::class.java.canonicalName.orEmpty())

    @Serializable
    data object Login : Destinations(Login::class.java.canonicalName.orEmpty())

    // MARK: Nested Navigation
    @Serializable
    data object Main : Destinations(Main::class.java.canonicalName.orEmpty())

    // MARK: Bottom Navigation Routes
    @Serializable
    data object Home : Destinations(Home::class.java.canonicalName.orEmpty())

    @Serializable
    data object Expenses : Destinations(Expenses::class.java.canonicalName.orEmpty())

    @Serializable
    data object Wallet : Destinations(Wallet::class.java.canonicalName.orEmpty())

    @Serializable
    data object Profile : Destinations(Profile::class.java.canonicalName.orEmpty())

    @Serializable
    data object AddExpenses : Destinations(AddExpenses::class.java.canonicalName.orEmpty())
}

