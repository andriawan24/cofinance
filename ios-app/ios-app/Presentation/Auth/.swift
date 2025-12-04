//
//  NavigationExamples.swift
//  ios-app
//
//  Quick Reference Guide for Navigation
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

// MARK: - Quick Reference Examples

/*
 
 ===================================
 NAVIGATION QUICK REFERENCE
 ===================================
 
 1. PUSH NAVIGATION (Stack-based)
 --------------------------------
 
 @EnvironmentObject var router: Router
 
 // Navigate forward
 router.navigate(to: .settings)
 router.navigate(to: .transactionDetail(id: "123"))
 
 // Navigate back
 router.navigateBack()
 
 // Pop to root
 router.navigateToRoot()
 
 
 2. SHEET PRESENTATION (Modal)
 --------------------------------
 
 // Present a sheet
 router.presentSheet(.addTransaction)
 router.presentSheet(.editProfile)
 
 // Dismiss sheet
 router.dismissSheet()
 
 
 3. FULL SCREEN COVER
 --------------------------------
 
 // Present full screen
 router.presentFullScreen(.onboarding)
 
 // Dismiss full screen
 router.dismissFullScreen()
 
 
 4. ALERTS
 --------------------------------
 
 // Simple alert
 router.showAlert(
     AlertItem(
         title: "Success",
         message: "Your changes have been saved"
     )
 )
 
 // Alert with actions
 router.showAlert(
     AlertItem(
         title: "Delete Item",
         message: "This action cannot be undone",
         primaryButton: AlertButton(
             title: "Delete",
             role: .destructive,
             action: { deleteItem() }
         ),
         secondaryButton: AlertButton(
             title: "Cancel",
             action: {}
         )
     )
 )
 
 
 5. ADDING NEW ROUTES
 --------------------------------
 
 Step 1: Add to Route enum in Router.swift
 
     enum Route: Hashable {
         case myNewScreen
         case detailScreen(id: String)
     }
 
 Step 2: Add view builder case in Router.swift
 
     func view(for route: Route) -> some View {
         switch route {
             case .myNewScreen:
                 MyNewScreenView()
             case .detailScreen(let id):
                 DetailScreenView(id: id)
         }
     }
 
 Step 3: Navigate to it
 
     router.navigate(to: .myNewScreen)
 
 
 6. ACCESSING ROUTER IN VIEWS
 --------------------------------
 
 struct MyView: View {
     @EnvironmentObject var router: Router
     
     var body: some View {
         Button("Navigate") {
             router.navigate(to: .settings)
         }
     }
 }
 
 
 7. LOGOUT FLOW
 --------------------------------
 
 @EnvironmentObject var authViewModel: AuthViewModel
 
 authViewModel.logout() // Automatically resets navigation
 
 
 8. COMMON PATTERNS
 --------------------------------
 
 // Navigate with validation
 if isValid {
     router.navigate(to: .nextScreen)
 } else {
     router.showAlert(AlertItem(
         title: "Error",
         message: "Please fill all fields"
     ))
 }
 
 // Save and go back
 saveData()
 router.navigateBack()
 
 // Navigate from list item
 List(items) { item in
     Button(item.title) {
         router.navigate(to: .detail(id: item.id))
     }
 }
 
 
 9. DEEP LINKING SETUP
 --------------------------------
 
 // Build a navigation path programmatically
 router.path = NavigationPath([
     Route.home,
     Route.category(id: "123"),
     Route.item(id: "456")
 ])
 
 
 10. DISMISSING VIEWS
 --------------------------------
 
 // Using router
 router.dismissSheet()
 router.dismissFullScreen()
 router.navigateBack()
 
 // Using environment dismiss
 @Environment(\.dismiss) var dismiss
 dismiss()
 
 */

// MARK: - Example View Implementation

struct NavigationExampleView: View {
    @EnvironmentObject var router: Router
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        VStack(spacing: 20) {
            // Push navigation
            Button("Push to Settings") {
                router.navigate(to: .settings)
            }
            
            // Sheet presentation
            Button("Present Sheet") {
                router.presentSheet(.addTransaction)
            }
            
            // Full screen
            Button("Show Full Screen") {
                router.presentFullScreen(.auth)
            }
            
            // Alert
            Button("Show Alert") {
                router.showAlert(
                    AlertItem(
                        title: "Hello",
                        message: "This is an alert"
                    )
                )
            }
            
            // Back navigation
            Button("Go Back") {
                router.navigateBack()
            }
            
            // Logout
            Button("Logout", role: .destructive) {
                router.showAlert(
                    AlertItem(
                        title: "Logout",
                        message: "Are you sure?",
                        primaryButton: AlertButton(
                            title: "Logout",
                            role: .destructive,
                            action: { authViewModel.logout() }
                        ),
                        secondaryButton: AlertButton(
                            title: "Cancel",
                            action: {}
                        )
                    )
                )
            }
        }
        .navigationTitle("Navigation Examples")
    }
}

// MARK: - List Navigation Example

struct ListNavigationExample: View {
    @EnvironmentObject var router: Router
    
    let items = ["Item 1", "Item 2", "Item 3"]
    
    var body: some View {
        List(items, id: \.self) { item in
            Button(item) {
                // Navigate with parameter
                router.navigate(to: .transactionDetail(id: item))
            }
        }
        .navigationTitle("Items")
    }
}

// MARK: - Form with Navigation Example

struct FormWithNavigationExample: View {
    @EnvironmentObject var router: Router
    @State private var name = ""
    @State private var isValid = false
    
    var body: some View {
        Form {
            TextField("Name", text: $name)
            
            Button("Submit") {
                if name.isEmpty {
                    router.showAlert(
                        AlertItem(
                            title: "Error",
                            message: "Please enter a name"
                        )
                    )
                } else {
                    // Save and navigate
                    saveData()
                    router.navigateBack()
                }
            }
        }
        .navigationTitle("Form Example")
    }
    
    func saveData() {
        // Save logic
    }
}
