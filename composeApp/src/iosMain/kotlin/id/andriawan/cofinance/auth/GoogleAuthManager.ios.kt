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
    fun onSuccess(idToken: String, email: String?)
    fun onError(message: String)
    fun onCancelled()
}

/**
 * Interface to be implemented by Swift GoogleSignInBridge
 */
interface GoogleSignInBridge {
    fun signIn(presentingViewController: platform.UIKit.UIViewController, callback: GoogleSignInCallback)
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

    actual fun signOut() {
        GoogleSignInBridgeHolder.bridge?.signOut()
    }

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
