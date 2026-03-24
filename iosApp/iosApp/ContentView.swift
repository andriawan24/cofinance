import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    var sharedImageUri: String?

    func makeUIViewController(context: Context) -> UIViewController {
        if let uri = sharedImageUri {
            return MainViewControllerKt.MainViewController(sharedImageUri: uri)
        }
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var sharedImageUri: String? = nil

    var body: some View {
        ComposeView(sharedImageUri: sharedImageUri)
            .ignoresSafeArea()
            .onOpenURL { url in
                if url.scheme == "cofinance" && url.host == "share" {
                    // URL format: cofinance://share?image=<file_path>
                    if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                       let imageParam = components.queryItems?.first(where: { $0.name == "image" })?.value {
                        sharedImageUri = imageParam
                    }
                }
            }
    }
}



