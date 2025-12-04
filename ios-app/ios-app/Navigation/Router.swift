//
//  Router.swift
//  ios-app
//
//  Created by Navigation System
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

enum Route: Hashable, Identifiable {
    case auth
    case login
    case home
    case activity
    case stats
    case account
    case profile
    case editProfile
    case settings
    case addTransaction
    case transactionHistory(categoryId: String)
    case attachmentViewer(url: String)
    case settingsNotifications
    case settingsPrivacy
    case settingsAbout
    var id: Self {
        self
    }
}

/// Router manages the navigation state for the entire app
@MainActor
class Router: ObservableObject {
    @Published var path = NavigationPath()
    @Published var presentedSheet: Route?
    @Published var presentedFullScreen: Route?
    @Published var alertItem: AlertItem?
    
    func navigate(to route: Route) {
        path.append(route)
    }
    
    func navigateBack() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }
    
    func navigateToRoot() {
        path = NavigationPath()
    }
    
    func presentSheet(_ route: Route) {
        presentedSheet = route
    }
    
    func dismissSheet() {
        presentedSheet = nil
    }
    
    func presentFullScreen(_ route: Route) {
        presentedFullScreen = route
    }
    
    func dismissFullScreen() {
        presentedFullScreen = nil
    }
    
    func showAlert(_ alert: AlertItem) {
        alertItem = alert
    }
    
    func dismissAlert() {
        alertItem = nil
    }
}

struct AlertItem: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let primaryButton: AlertButton?
    let secondaryButton: AlertButton?
    
    init(
        title: String,
        message: String,
        primaryButton: AlertButton? = AlertButton(title: "OK", action: {}),
        secondaryButton: AlertButton? = nil
    ) {
        self.title = title
        self.message = message
        self.primaryButton = primaryButton
        self.secondaryButton = secondaryButton
    }
}

struct AlertButton {
    let title: String
    let role: ButtonRole?
    let action: () -> Void
    
    init(title: String, role: ButtonRole? = nil, action: @escaping () -> Void) {
        self.title = title
        self.role = role
        self.action = action
    }
}

extension Router {
    @ViewBuilder
    func view(for route: Route) -> some View {
        switch route {
        case .auth:
            AuthView()
        case .login:
            AuthView()
        case .home:
            HomeView()
        case .activity:
            ActivityView()
        case .stats:
            StatsView()
        case .account:
            AccountView()
        case .profile:
            AccountView()
        case .editProfile:
            EditProfileView()
        case .settings:
            SettingsView()
        case .addTransaction:
            Text("Add Transaction")
                .navigationTitle("New Transaction")
        case .transactionHistory(let categoryId):
            Text("Transaction History for category: \(categoryId)")
                .navigationTitle("History")
        case .attachmentViewer(let url):
            Text("Viewing attachment: \(url)")
                .navigationTitle("Attachment")
        case .settingsNotifications:
            NotificationSettingsView()
        case .settingsPrivacy:
            PrivacySettingsView()
        case .settingsAbout:
            AboutView()
        }
    }
}

struct RouterView<Content: View>: View {
    @ObservedObject var router: Router
    let content: () -> Content
    
    var body: some View {
        NavigationStack(path: $router.path) {
            content()
                .navigationDestination(for: Route.self) { route in
                    router.view(for: route)
                }
        }
        .sheet(item: $router.presentedSheet) { route in
            NavigationStack {
                router.view(for: route)
            }
        }
        .fullScreenCover(item: $router.presentedFullScreen) { route in
            NavigationStack {
                router.view(for: route)
            }
        }
        .alert(item: $router.alertItem) { alertItem in
            Alert(
                title: Text(alertItem.title),
                message: Text(alertItem.message),
                primaryButton: alertItem.primaryButton.map { button in
                    .default(Text(button.title), action: button.action)
                } ?? .default(Text("OK")),
                secondaryButton: alertItem.secondaryButton.map { button in
                    .cancel(Text(button.title), action: button.action)
                } ?? .cancel()
            )
        }
    }
}
