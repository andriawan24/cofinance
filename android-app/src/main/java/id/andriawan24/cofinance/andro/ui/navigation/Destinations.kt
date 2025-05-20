package id.andriawan24.cofinance.andro.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destinations(val route: String) {
    @Serializable
    data object Splash : Destinations(Splash::class.java.canonicalName.orEmpty())

    @Serializable
    data object Login : Destinations(Login::class.java.canonicalName.orEmpty())

    // MARK: Nested Navigation
    @Serializable
    data object Main : Destinations(Main::class.java.canonicalName.orEmpty())

    // MARK: Bottom Navigation Routes
    @Serializable
    data object Activity : Destinations(Activity::class.java.canonicalName.orEmpty())

    @Serializable
    data object Budget : Destinations(Budget::class.java.canonicalName.orEmpty())

    @Serializable
    data object Account : Destinations(Account::class.java.canonicalName.orEmpty())

    @Serializable
    data object Profile : Destinations(Profile::class.java.canonicalName.orEmpty())

    @Serializable
    data class AddNew(
        val totalPrice: Long = 0,
        val date: String = ""
    ) : Destinations(AddNew::class.java.canonicalName.orEmpty())

    @Serializable
    data object Camera : Destinations(Camera::class.java.canonicalName.orEmpty())

    @Serializable
    data class Preview(
        val imageUrl: String
    ) : Destinations(Preview::class.java.canonicalName.orEmpty())
}

