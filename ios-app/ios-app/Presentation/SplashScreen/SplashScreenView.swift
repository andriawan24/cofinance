//
//  SplashScreen.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 12/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SplashScreenView: View {
    var body: some View {
        ZStack {
            Color("PrimaryColor")
                .ignoresSafeArea()
            
            Text("Cofinance")
                .primaryText(fontName: "Manrope-Bold")
        }
    }
}

#Preview {
    SplashScreenView()
}
