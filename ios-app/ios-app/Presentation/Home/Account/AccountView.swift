//
//  AccountView.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 17/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct AccountView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        List {
            Section("Profile") {
                Button {
                    router.presentFullScreen(.editProfile)
                } label: {
                    HStack {
                        Label("Edit Profile", systemImage: "person.circle")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
                
                Button {
                    router.navigate(to: .settings)
                } label: {
                    HStack {
                        Label("Settings", systemImage: "gear")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            Section("Actions") {
                Button(role: .destructive) {
                    router.showAlert(
                        AlertItem(
                            title: "Logout",
                            message: "Are you sure you want to logout?",
                            primaryButton: AlertButton(
                                title: "Logout",
                                role: .destructive,
                                action: {
                                    
                                }
                            ),
                            secondaryButton: AlertButton(
                                title: "Cancel",
                                action: {}
                            )
                        )
                    )
                } label: {
                    Label("Logout", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .navigationTitle("Account")
    }
}

#Preview {
    let authVM = AuthViewModel()
    let coordinator = AppCoordinator(authViewModel: authVM)
    
    return NavigationStack {
        AccountView()
            .environmentObject(authVM)
            .environmentObject(coordinator.router)
    }
}
