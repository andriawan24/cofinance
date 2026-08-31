import Foundation
import UIKit
import FirebaseCore
import GoogleSignIn
import ComposeApp

/// Native GoogleSignIn implementation of the Kotlin `GoogleSignInBridge`.
///
/// Keeping the SDK call here means the shared Kotlin module needs no cinterop
/// for GoogleSignIn - it only sees the bridge interface.
final class GoogleSignInHelper: NSObject, GoogleSignInBridge {

    /// Configures GIDSignIn with the client ID from GoogleService-Info.plist.
    /// Call after `FirebaseApp.configure()`.
    static func configure() {
        guard let clientID = FirebaseApp.app()?.options.clientID else { return }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
    }

    func signIn(callback: @escaping (GoogleSignInBridgeResult) -> Void) {
        DispatchQueue.main.async {
            guard let presenting = Self.topViewController() else {
                callback(Self.failure("No view controller available to present Google Sign-In"))
                return
            }

            GIDSignIn.sharedInstance.signIn(withPresenting: presenting) { result, error in
                if let error = error as NSError? {
                    if error.code == GIDSignInError.canceled.rawValue {
                        callback(
                            GoogleSignInBridgeResult(
                                idToken: nil,
                                accessToken: nil,
                                email: nil,
                                errorMessage: nil,
                                cancelled: true
                            )
                        )
                    } else {
                        callback(Self.failure(error.localizedDescription))
                    }
                    return
                }

                guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                    callback(Self.failure("Failed to get ID token from Google"))
                    return
                }

                callback(
                    GoogleSignInBridgeResult(
                        idToken: idToken,
                        accessToken: user.accessToken.tokenString,
                        email: user.profile?.email,
                        errorMessage: nil,
                        cancelled: false
                    )
                )
            }
        }
    }

    private static func failure(_ message: String) -> GoogleSignInBridgeResult {
        GoogleSignInBridgeResult(
            idToken: nil,
            accessToken: nil,
            email: nil,
            errorMessage: message,
            cancelled: false
        )
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }

        guard let root = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            return nil
        }

        var current = root
        while let presented = current.presentedViewController {
            current = presented
        }
        return current
    }
}
