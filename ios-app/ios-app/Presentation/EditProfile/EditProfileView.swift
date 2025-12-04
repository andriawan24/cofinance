//
//  EditProfileView.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 17/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct EditProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var router: Router
    @Environment(\.dismiss) var dismiss
    
    @State private var name = "John Doe"
    @State private var email = "john@example.com"
    @State private var phoneNumber = "+1 234 567 890"
    
    var body: some View {
        Form {
            Section("Personal Information") {
                TextField("Name", text: $name)
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                TextField("Phone", text: $phoneNumber)
                    .keyboardType(.phonePad)
                    .textContentType(.telephoneNumber)
            }
            
            Section("Navigation Examples") {
                // Example: Navigate deeper from this view (Level 2)
                Button {
                    router.navigate(to: .settings)
                } label: {
                    HStack {
                        Label("App Settings", systemImage: "gear")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
                .foregroundColor(.primary)
            }
            
            Section {
                Button("Save Changes") {
                    saveProfile()
                    dismiss() // Go back after saving
                }
                
                Button("Save and Go to Home") {
                    saveProfile()
                    router.navigateToRoot() // Go all the way back to root
                }
            }
            
            Section {
                Button("Cancel") {
                    dismiss()
                }
                .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    dismiss()
                }
            }
            
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    saveProfile()
                    dismiss()
                }
            }
        }
    }
    
    private func saveProfile() {
        // Save profile logic here
        print("Profile saved: \(name), \(email), \(phoneNumber)")
    }
}

#Preview {
    let authVM = AuthViewModel()
    let coordinator = AppCoordinator(authViewModel: authVM)
    
    return NavigationStack {
        EditProfileView()
            .environmentObject(authVM)
            .environmentObject(coordinator.router)
    }
}
