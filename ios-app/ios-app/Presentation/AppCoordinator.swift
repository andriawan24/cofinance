//
//  AppCoordinator.swift
//  ios-app
//
//  Created by Navigation System
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Combine
import shared

@MainActor
class AppCoordinator: ObservableObject {
    @Published var appState: AppState = .splash
    @Published var router = Router()
    
    let authViewModel: AuthViewModel
    private let fetchUserUseCase: FetchUserUseCase

    private var cancellables = Set<AnyCancellable>()
    
    init(authViewModel: AuthViewModel) {
        self.authViewModel = authViewModel
        
        let koinHelper = KoinHelper()
        self.fetchUserUseCase = koinHelper.getFetchUserUseCase()
        
        observe()
    }
    
    private func observe() {
        authViewModel.$isSuccessLogin.receive(on: DispatchQueue.main)
            .sink { [weak self] isLoggedIn in
                if isLoggedIn {
                    self?.appState = .authenticated
                }
            }
            .store(in: &cancellables)
    }
    
    func start() {
        let fetchUser = self.fetchUserUseCase.execute().toPublisher()
        fetchUser
            .receive(on: DispatchQueue.main)
            .sink { [weak self] result in
                switch onEnum(of: result) {
                case .loading:
                    break
                case .success:
                    self?.appState = .authenticated
                case .error(let error):
                    print(error.exception.message ?? "")
                    self?.appState = .unauthenticated
                }
            }.store(in: &cancellables)
    }
    
    private func handleSuccessfulLogin() {
        withAnimation {
            appState = .authenticated
            router.navigateToRoot() // Clear any existing navigation
        }
    }
    
    private func handleLogout() {
        withAnimation {
            appState = .unauthenticated
            router.navigateToRoot() // Clear navigation stack
        }
    }
    
    func logout() {
        authViewModel.logout()
    }
}

enum AppState {
    case splash
    case unauthenticated
    case authenticated
}
