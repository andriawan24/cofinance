//
//  HomePage.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 17/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct HomeView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var router: Router
    @State private var selectedTab = "Activity"
    
    var body: some View {
        RouterView(router: router) {
            ZStack {
                TabView(selection: $selectedTab) {
                    ActivityView()
                        .background(Color("Background"))
                        .tag("Activity")
                        .tabItem {
                            Label("Activity", systemImage: "house.fill")
                        }
                    
                    StatsView()
                        .background(Color("Background"))
                        .tag("Stats")
                        .tabItem {
                            Label("Stats", systemImage: "chart.pie")
                        }
                    
                    Spacer()
                    
                    AccountView()
                        .background(Color("Background"))
                        .tag("Account")
                        .tabItem {
                            Label("Account", systemImage: "singaporedollarsign.bank.building")
                        }
                    
                    ProfileView()
                        .background(Color("Background"))
                        .tag("Profile")
                        .tabItem {
                            Label("Profile", systemImage: "person.crop.circle")
                        }
                }
                .onAppear {
                    let standardAppearance = UITabBarAppearance()
                    standardAppearance.shadowColor = UIColor(Color.black)
                    UITabBar.appearance().standardAppearance = standardAppearance
                }
                
                VStack {
                    Spacer()
                    
                    Button {
                        router.navigate(to: .addTransaction)
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .resizable()
                            .foregroundColor(.accentColor)
                            .frame(width: 50, height: 50)
                    }
                }
            }
            .onAppear {
                UINavigationBar.appearance().largeTitleTextAttributes = [
                    .font : UIFont(name: "Manrope-Bold", size: 32)!
                ]
            }
        }
    }
}

#Preview {
    let authVM = AuthViewModel()
    let coordinator = AppCoordinator(authViewModel: authVM)
    
    return HomeView()
        .environmentObject(authVM)
        .environmentObject(coordinator.router)
}
