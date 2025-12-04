//
//  SettingsView.swift
//  ios-app
//
//  Created by Navigation System
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

/// Example: Level 1 Settings View
/// Demonstrates deep navigation with multiple sub-pages
struct SettingsView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        List {
            Section("Preferences") {
                // Navigate to Level 2 - Notifications
                Button {
                    router.navigate(to: .settingsNotifications)
                } label: {
                    HStack {
                        Label("Notifications", systemImage: "bell.fill")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
                
                // Navigate to Level 2 - Privacy
                Button {
                    router.navigate(to: .settingsPrivacy)
                } label: {
                    HStack {
                        Label("Privacy", systemImage: "lock.fill")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            Section("App") {
                // Navigate to Level 2 - About
                Button {
                    router.navigate(to: .settingsAbout)
                } label: {
                    HStack {
                        Label("About", systemImage: "info.circle.fill")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            Section {
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
        .navigationTitle("Settings")
    }
}

/// Example: Level 2 - Notification Settings
struct NotificationSettingsView: View {
    @State private var pushEnabled = true
    @State private var emailEnabled = false
    @State private var transactionAlerts = true
    
    var body: some View {
        Form {
            Section("Push Notifications") {
                Toggle("Enable Push Notifications", isOn: $pushEnabled)
                Toggle("Transaction Alerts", isOn: $transactionAlerts)
                    .disabled(!pushEnabled)
            }
            
            Section("Email") {
                Toggle("Email Notifications", isOn: $emailEnabled)
            }
            
            Section {
                Text("Manage how you receive notifications from the app")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Example: Level 2 - Privacy Settings
struct PrivacySettingsView: View {
    @State private var shareAnalytics = false
    @State private var biometricAuth = true
    
    var body: some View {
        Form {
            Section("Security") {
                Toggle("Biometric Authentication", isOn: $biometricAuth)
            }
            
            Section("Data") {
                Toggle("Share Anonymous Analytics", isOn: $shareAnalytics)
            }
            
            Section {
                Button("View Privacy Policy") {
                    // Open privacy policy
                }
                
                Button("Export My Data") {
                    // Export data
                }
                
                Button("Delete Account", role: .destructive) {
                    // Delete account
                }
            }
        }
        .navigationTitle("Privacy")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Example: Level 2 - About
struct AboutView: View {
    var body: some View {
        Form {
            Section("App Info") {
                LabeledContent("Version", value: "1.0.0")
                LabeledContent("Build", value: "100")
            }
            
            Section("Legal") {
                Button("Terms of Service") {
                    // Open terms
                }
                
                Button("Privacy Policy") {
                    // Open privacy
                }
                
                Button("Licenses") {
                    // Show licenses
                }
            }
            
            Section("Support") {
                Button("Contact Support") {
                    // Open support
                }
                
                Button("Rate on App Store") {
                    // Open App Store
                }
            }
        }
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("Settings") {
    let authVM = AuthViewModel()
    let coordinator = AppCoordinator(authViewModel: authVM)
    
    return NavigationStack {
        SettingsView()
            .environmentObject(authVM)
            .environmentObject(coordinator.router)
    }
}

#Preview("Notifications") {
    NavigationStack {
        NotificationSettingsView()
    }
}

#Preview("Privacy") {
    NavigationStack {
        PrivacySettingsView()
    }
}

#Preview("About") {
    NavigationStack {
        AboutView()
    }
}
