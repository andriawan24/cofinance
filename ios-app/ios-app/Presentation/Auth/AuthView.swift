//
//  AuthView.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 10/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import GoogleSignIn

struct AuthView: View {
    // MARK: - Properties
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var router: Router
    @State private var selectedTab = 0
    private let numberOfPages = 3
    
    // MARK: - Body
    var body: some View {
        ZStack {
            backgroundGradient
            
            VStack(spacing: 16) {
                logoView
                onboardingPager
                pageIndicator
    
                Spacer()
                
                signInButtons
            }
        }
        .onOpenURL { url in
            GIDSignIn.sharedInstance.handle(url)
        }
        .task {
            
        }
    }
    
    // MARK: - Background
    private var backgroundGradient: some View {
        LinearGradient(
            colors: [
                Color("BackgroundTop"),
                Color("BackgroundBottom")
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
    
    private var logoView: some View {
        Image("ic_logo")
            .padding(.top)
    }
    
    private var onboardingPager: some View {
        TabView(selection: $selectedTab) {
            firstPage.tag(0)
            secondPage.tag(1)
            thirdPage.tag(2)
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
    }
    
    private var firstPage: some View {
        VStack {
            Image("img_onboarding")
                .resizable()
                .aspectRatio(contentMode: .fit)
            
            Text("Track your money with")
                .primaryText(fontName: "Manrope-Bold")
            
            Text("Cofinance")
                .multilineTextAlignment(.center)
                .primaryText(
                    fontName: "Manrope-ExtraBold",
                    color: Color("PrimaryColor")
                )
        }
    }
    
    private var secondPage: some View {
        VStack {
            Text("Second")
        }
    }
    
    private var thirdPage: some View {
        VStack {
            Text("Third")
        }
    }
    
    private var pageIndicator: some View {
        SlimePageIndicator(
            currentPage: $selectedTab,
            numberOfPages: numberOfPages
        )
    }
    
    private var signInButtons: some View {
        VStack(spacing: 12) {
            appleSignInButton
            googleSignInButton
        }
        .padding(.horizontal)
    }
    
    private var appleSignInButton: some View {
        Button {
            // TODO: Handle on sign in google click
        } label: {
            HStack {
                Spacer()
                Image(systemName: "apple.logo")
                    .aspectRatio(contentMode: .fit)
                Text("Sign in with apple")
                    .font(.custom("Manrope-Bold", size: 16))
                Spacer()
            }
            .padding(.vertical, 16)
        }
        .buttonStyle(.borderedProminent)
        .clipShape(Capsule())
        .tint(Color("PrimaryColor"))
    }
    
    private var googleSignInButton: some View {
        Button {
            authViewModel.login { idToken in
                authViewModel.handleIdToken(idToken: idToken)
            }
        } label: {
            HStack {
                Spacer()
                Image("ic_google_logo")
                    .aspectRatio(contentMode: .fit)
                Text("Sign in with google")
                    .font(.custom("Manrope-Bold", size: 16))
                Spacer()
            }
            .padding(.vertical, 16)
        }
        .buttonStyle(.borderedProminent)
        .clipShape(Capsule())
        .tint(Color("PrimaryColor"))
    }
}

// MARK: - Preview
#Preview {
    AuthView()
}
