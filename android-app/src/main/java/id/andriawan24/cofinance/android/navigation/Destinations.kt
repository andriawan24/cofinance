package id.andriawan24.cofinance.android.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destinations(val route: String) {
    @Serializable
    data object Onboarding : Destinations(Onboarding::class.java.canonicalName)

    @Serializable
    data object Login : Destinations(Login::class.java.canonicalName)

    // MARK: Nested Navigation
    @Serializable
    data object Main : Destinations(Main::class.java.canonicalName)

    // MARK: Bottom Navigation Routes
    @Serializable
    data object Home : Destinations(Home::class.java.canonicalName)

    @Serializable
    data object Expenses : Destinations(Expenses::class.java.canonicalName)

    @Serializable
    data object Wallet : Destinations(Wallet::class.java.canonicalName)

    @Serializable
    data object Profile : Destinations(Profile::class.java.canonicalName)
}

