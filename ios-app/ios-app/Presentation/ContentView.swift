import SwiftUI
import shared

struct ContentView: View {
    @EnvironmentObject var coordinator: AppCoordinator
    @EnvironmentObject var router: Router
    
    var body: some View {
        ZStack {
            switch coordinator.appState {
            case .splash:
                SplashScreenView()
                
            case .unauthenticated:
                AuthView()
                
            case .authenticated:
                HomeView()
            }
        }
        .onAppear {
            coordinator.start()
        }
    }
}

#Preview {
    let authVM = AuthViewModel()
    let coordinator = AppCoordinator(authViewModel: authVM)
    
    return ContentView()
        .environmentObject(authVM)
        .environmentObject(coordinator)
        .environmentObject(coordinator.router)
}
