package id.andriawan.cofinance.navigations.destinations

import kotlinx.serialization.Serializable

@Serializable
sealed class Destinations(val route: String) {
    @Serializable
    data object Splash : Destinations(Splash::class.simpleName.orEmpty())

    @Serializable
    data object Login : Destinations(Login::class.simpleName.orEmpty())

    @Serializable
    data class AddNew(
        val transactionId: String? = null,
        /** Comma-joined `ReceiptField` names the scan was unsure about, or null. */
        val lowConfidenceFields: String? = null
    ) : Destinations(AddNew::class.simpleName.orEmpty())

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
    data object EditProfile : Destinations(EditProfile::class.simpleName.orEmpty())

    @Serializable
    data object Profile : Destinations(Profile::class.simpleName.orEmpty())

    @Serializable
    data object CycleReview : Destinations(CycleReview::class.simpleName.orEmpty())

    /** Mandatory encryption setup, reached from sign-in and never by a local-only user. */
    @Serializable
    data object EncryptionSetup : Destinations(EncryptionSetup::class.simpleName.orEmpty())

    /**
     * The app lock's unlock screen, reached only by a session that holds key material it cannot
     * open. A user who never completed encryption setup has no route to it.
     */
    @Serializable
    data object Unlock : Destinations(Unlock::class.simpleName.orEmpty())

    /** Recovery-phrase restore, for a signed-in device that holds no key material. */
    @Serializable
    data object RecoveryPhraseRestore :
        Destinations(RecoveryPhraseRestore::class.simpleName.orEmpty())
}

