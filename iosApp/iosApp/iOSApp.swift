import SwiftUI
import FirebaseCore
import FirebaseAuth
import GoogleSignIn
import ComposeApp

@main
struct iOSApp: App {

    init() {
        FirebaseApp.configure()
        GoogleSignInHelper.configure()
        GoogleSignInBridgeRegistry.shared.bridge = GoogleSignInHelper()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // cofinance:// scheme URLs are handled by ContentView's onOpenURL
                    guard url.scheme != "cofinance" else { return }

                    if GIDSignIn.sharedInstance.handle(url) { return }
                    _ = Auth.auth().canHandle(url)
                }
        }
    }
}
