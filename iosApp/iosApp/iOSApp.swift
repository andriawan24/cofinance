import SwiftUI
import FirebaseCore
import FirebaseAuth

@main
struct iOSApp: App {

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Firebase Auth handles its own OAuth callback URLs
                    if url.scheme != "cofinance" {
                        _ = Auth.auth().canHandle(url)
                    }
                    // cofinance:// scheme URLs are handled by ContentView's onOpenURL
                }
        }
    }
}
