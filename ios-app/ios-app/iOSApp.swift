import SwiftUI
import shared

@main
struct iOSApp: App {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var coordinator: AppCoordinator

    init() {
        // Initialize Koin for dependency injection
        KoinHelperKt.doInitKoin()

        let authVM = AuthViewModel()
        let appCoordinator = AppCoordinator(authViewModel: authVM)

        _authViewModel = StateObject(wrappedValue: authVM)
        _coordinator = StateObject(wrappedValue: appCoordinator)
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
                .environmentObject(authViewModel)
                .environmentObject(coordinator)
                .environmentObject(coordinator.router)
		}
	}
}
