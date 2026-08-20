package id.andriawan.cofinance.auth

/**
 * Outcome of a native GoogleSignIn attempt, produced by the Swift side.
 *
 * Exactly one of [idToken], [errorMessage] or [cancelled] is meaningful:
 * a non-null [idToken] means success, [cancelled] means the user dismissed
 * the sheet, otherwise [errorMessage] describes the failure.
 */
data class GoogleSignInBridgeResult(
    val idToken: String?,
    val email: String?,
    val errorMessage: String?,
    val cancelled: Boolean
)

/**
 * Implemented in Swift ([iosApp/iosApp/GoogleSignInHelper.swift]) so the native
 * GoogleSignIn SDK stays on the Xcode/SPM side and Kotlin needs no cinterop.
 */
interface GoogleSignInBridge {
    fun signIn(callback: (GoogleSignInBridgeResult) -> Unit)
}

/** Holder for the Swift implementation, wired up during app startup. */
object GoogleSignInBridgeRegistry {
    var bridge: GoogleSignInBridge? = null
}
