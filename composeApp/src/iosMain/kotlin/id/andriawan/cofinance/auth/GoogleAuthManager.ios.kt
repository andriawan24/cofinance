package id.andriawan.cofinance.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import kotlin.coroutines.resume

/**
 * Callback interface for Google Sign-In results from Swift
 */
interface GoogleSignInCallback {
    /**
 * Invoked when Google sign-in completes successfully with the authenticated user's credentials.
 *
 * @param idToken The ID token returned by Google for the authenticated user.
 * @param email The user's email address, or `null` if not available.
 */
fun onSuccess(idToken: String, email: String?)
    /**
 * Called when the sign-in flow fails.
 *
 * @param message A human-readable error message describing the failure.
 */
fun onError(message: String)
    /**
 * Called when the Google Sign-In flow is cancelled by the user.
 */
fun onCancelled()
}

/**
 * Interface to be implemented by Swift GoogleSignInBridge
 */
interface GoogleSignInBridge {
    /**
 * Starts the Google Sign-In flow using the given view controller to present the sign-in UI.
 *
 * @param presentingViewController The UIViewController used to present the Google Sign-In UI.
 * @param callback Callback that receives sign-in outcomes: `onSuccess(idToken, email)` when sign-in succeeds, `onError(message)` when an error occurs, and `onCancelled()` when the user cancels.
 */
fun signIn(presentingViewController: platform.UIKit.UIViewController, callback: GoogleSignInCallback)
    /**
 * Signs out the currently authenticated Google user via the configured iOS bridge.
 *
 * If no bridge is configured, this call has no effect.
 */
fun signOut()
}

/**
 * Singleton holder for the Swift bridge instance
 * Must be set from Swift/iOS app initialization
 */
object GoogleSignInBridgeHolder {
    var bridge: GoogleSignInBridge? = null
}

@OptIn(ExperimentalForeignApi::class)
actual class GoogleAuthManager {

    /**
     * Initiates the Google Sign-In flow and produces a `GoogleAuthResult` reflecting the outcome.
     *
     * @return `GoogleAuthResult.Success` containing the ID token and optional email when sign-in succeeds;
     * `GoogleAuthResult.Error` with an error message when sign-in fails or when the bridge is not configured or the root view controller cannot be found;
     * `GoogleAuthResult.Cancelled` when the user cancels the sign-in flow.
     */
    actual suspend fun signIn(): GoogleAuthResult = suspendCancellableCoroutine { continuation ->
        val bridge = GoogleSignInBridgeHolder.bridge
        if (bridge == null) {
            continuation.resume(
                GoogleAuthResult.Error("Google Sign-In not configured. Please initialize GoogleSignInBridge.")
            )
            return@suspendCancellableCoroutine
        }

        val rootViewController = getRootViewController()
        if (rootViewController == null) {
            continuation.resume(
                GoogleAuthResult.Error("Unable to find root view controller")
            )
            return@suspendCancellableCoroutine
        }

        bridge.signIn(rootViewController, object : GoogleSignInCallback {
            override fun onSuccess(idToken: String, email: String?) {
                if (continuation.isActive) {
                    continuation.resume(GoogleAuthResult.Success(idToken, email))
                }
            }

            override fun onError(message: String) {
                if (continuation.isActive) {
                    continuation.resume(GoogleAuthResult.Error(message))
                }
            }

            override fun onCancelled() {
                if (continuation.isActive) {
                    continuation.resume(GoogleAuthResult.Cancelled)
                }
            }
        })
    }

    /**
     * Signs the current user out of Google using the configured iOS bridge.
     *
     * If a bridge has been installed in GoogleSignInBridgeHolder, calls its `signOut()` implementation;
     * otherwise this function is a no-op.
     */
    actual fun signOut() {
        GoogleSignInBridgeHolder.bridge?.signOut()
    }

    /**
     * Finds the first non-null root UIViewController by inspecting the application's connected scenes and their windows.
     *
     * @return The first root `UIViewController` found, or `null` if no root view controller is available.
     */
    @OptIn(ExperimentalForeignApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun getRootViewController(): UIViewController? {
        val connectedScenes = UIApplication.sharedApplication.connectedScenes
        for (scene in connectedScenes) {
            if (scene is UIWindowScene) {
                val windows = scene.windows as List<UIWindow>
                for (window in windows) {
                    val viewController = window.rootViewController
                    if (viewController != null) {
                        return viewController
                    }
                }
            }
        }
        return null
    }
}