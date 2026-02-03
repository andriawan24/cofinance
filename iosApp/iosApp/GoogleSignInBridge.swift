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

    /// Initialize Google Sign-In and register the bridge with Kotlin
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

    /// Handle URL callback from Google Sign-In
    public static func handleURL(_ url: URL) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }

    // MARK: - GoogleSignInBridge Protocol

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

    public func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }
}

/// Extension to restore previous sign-in on app launch
extension GoogleSignInBridgeImpl {

    /// Attempt to restore previous sign-in session
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
