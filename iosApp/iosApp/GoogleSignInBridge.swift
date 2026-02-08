import Foundation
import UIKit
import GoogleSignIn
import ComposeApp

/// Swift implementation of GoogleSignInBridge protocol from Kotlin
/// Handles native Google Sign-In SDK calls
@objc public class GoogleSignInBridgeImpl: NSObject, GoogleSignInBridge {

    public static let shared = GoogleSignInBridgeImpl()

    private override init() {
        super.init()
    }

    /// Configures Google Sign-In for the app using the `GIDClientID` value from Info.plist.
    /// Reads the `GIDClientID` key from the main bundle; if present, initializes and assigns the Google Sign-In configuration and registers this bridge with the Kotlin bridge holder. If the key is missing, logs an error and does not configure Google Sign-In.
    public static func configure() {
        // Get client ID from Info.plist
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String else {
            print("GoogleSignIn: Missing GIDClientID in Info.plist")
            return
        }

        // Configure Google Sign-In
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        // Register bridge with Kotlin
        GoogleSignInBridgeHolder.shared.bridge = shared

        print("GoogleSignIn: Configured successfully with client ID")
    }

    /// Forwards an incoming URL to Google Sign-In for handling.
    /// - Parameters:
    ///   - url: The URL to be processed (typically received by the app from an external callback).
    /// - Returns: `true` if Google Sign-In handled the URL, `false` otherwise.
    public static func handleURL(_ url: URL) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }

    /// Initiates the Google Sign-In flow using the provided view controller.
    /// 
    /// Attempts to sign the user in with Google and reports the outcome through the callback:
    /// - On success: delivers the user's ID token and optional email via `callback.onSuccess`.
    /// - On cancellation: invokes `callback.onCancelled`.
    /// - On failure: invokes `callback.onError` with a descriptive message.
    /// - Parameters:
    ///   - presentingViewController: The view controller used to present the Google Sign-In UI.
    ///   - callback: The callback to receive success, cancellation, or error events.

    public func signIn(presentingViewController: UIViewController, callback: any GoogleSignInCallback) {
        // Check if configuration exists
        guard GIDSignIn.sharedInstance.configuration != nil else {
            callback.onError(message: "Google Sign-In not configured. Call GoogleSignInBridgeImpl.configure() first.")
            return
        }

        // Perform sign-in
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            // Handle errors
            if let error = error as NSError? {
                // Check if user cancelled
                if error.code == GIDSignInError.canceled.rawValue {
                    callback.onCancelled()
                    return
                }

                callback.onError(message: error.localizedDescription)
                return
            }

            // Get user and ID token
            guard let user = result?.user,
                  let idToken = user.idToken?.tokenString else {
                callback.onError(message: "Failed to get ID token from Google")
                return
            }

            let email = user.profile?.email
            callback.onSuccess(idToken: idToken, email: email)
        }
    }

    /// Signs out the currently authenticated Google user and clears the local Google Sign-In state.
    public func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }
}

/// Extension to restore previous sign-in on app launch
extension GoogleSignInBridgeImpl {

    /// Attempts to restore a previously authenticated Google Sign-In session.
    /// - Parameter completion: Called with `true` if a previous session was successfully restored; called with `false` if no previous session exists or restoration failed.
    public func restorePreviousSignIn(completion: @escaping (Bool) -> Void) {
        GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
            if let error = error {
                print("GoogleSignIn: Failed to restore previous sign-in: \(error.localizedDescription)")
                completion(false)
                return
            }

            if user != nil {
                print("GoogleSignIn: Restored previous sign-in")
                completion(true)
            } else {
                print("GoogleSignIn: No previous sign-in to restore")
                completion(false)
            }
        }
    }
}