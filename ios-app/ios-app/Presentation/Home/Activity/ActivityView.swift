//
//  ActivityView.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 17/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ActivityView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Activity")
                    .primaryText(
                        fontName: "Manrope-Bold",
                        size: 24
                    )
                
                Spacer()
            }
            .padding(.horizontal)
            .padding(.top)
            
            MonthSwitcher().padding(.vertical, 8)
            
            BalanceCard()
            
            ScrollView {
                LazyVStack(spacing: 24) {
                    TransactionDayItem()
                    TransactionDayItem()
                    Spacer()
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        ActivityView()
    }
}

