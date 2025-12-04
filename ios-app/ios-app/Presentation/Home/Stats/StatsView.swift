//
//  StatsView.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 17/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct StatsView: View {
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Statistic")
                    .primaryText(
                        fontName: "Manrope-Bold",
                        size: 24
                    )
                
                Spacer()
            }
            .padding(.horizontal)
            .padding(.top)
            
            MonthSwitcherStats().padding(.vertical)
            
            VStack {
                HStack {
                    VStack(alignment: .leading) {
                        Text("Total Expenses")
                        Text(10_000_000.asFormatted)
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .background(.white)
            .padding(.horizontal)
            
            Spacer()
        }
    }
}

#Preview {
    StatsView()
        .background(Color("Background"))
}
