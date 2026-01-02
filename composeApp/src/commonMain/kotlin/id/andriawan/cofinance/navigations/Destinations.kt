package id.andriawan.cofinance.navigations

import kotlinx.serialization.Serializable

@Serializable
sealed class Destinations(val route: String) {
    @Serializable
    data object Splash : Destinations(Splash::class.simpleName.orEmpty())

    @Serializable
    data object Login : Destinations(Login::class.simpleName.orEmpty())

    @Serializable
    data class AddNew(val transactionId: String? = null) : Destinations(AddNew::class.simpleName.orEmpty())

    @Serializable
    data object Camera : Destinations(Camera::class.simpleName.orEmpty())

    @Serializable
    data class Preview(val imageUrl: String) : Destinations(Preview::class.simpleName.orEmpty())

    @Serializable
    data object Main : Destinations(Main::class.simpleName.orEmpty())

    @Serializable
    data object Activity : Destinations(Activity::class.simpleName.orEmpty())

    @Serializable
    data object Stats : Destinations(Stats::class.simpleName.orEmpty())

    @Serializable
    data object Account : Destinations(Account::class.simpleName.orEmpty())

    @Serializable
    data object AddAccount : Destinations(AddAccount::class.simpleName.orEmpty())

    @Serializable
    data object Profile : Destinations(Profile::class.simpleName.orEmpty())
}

