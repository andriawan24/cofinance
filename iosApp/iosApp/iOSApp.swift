import SwiftUI

@main
struct iOSApp: App {

    init() {
        // Configure Google Sign-In on app launch
        GoogleSignInBridgeImpl.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle Google Sign-In URL callback
                    if url.scheme != "cofinance" {
                        _ = GoogleSignInBridgeImpl.handleURL(url)
                    }
                    // cofinance:// scheme URLs are handled by ContentView's onOpenURL
                }
        }
    }
}
