//
//  TransactionDayItem.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 21/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct TransactionDayItem: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Wednesday, 15 May 2025")
                .primaryText(
                    fontName: "Manrope-SemiBold",
                    size: 14
                )
            
            VStack(alignment: .leading, spacing: 0) {
                IconBalanceCardHorizontal(
                    image: "ic_income_small",
                    label: "Incomes",
                    value: 11000000
                )
                
                Divider()
                    .frame(height: 1)
                    .background(Color("DividerColor"))
                    .padding(.horizontal, 12)
                
                IconBalanceCardHorizontal(
                    image: "ic_income_small",
                    label: "Expenses",
                    value: 50000000
                )
            }
            .background(Color("BackgroundGrey"))
            .clipShape(RoundedRectangle(cornerRadius: 4))
            .padding(.top, 8)
            
            Divider()
                .frame(height: 1)
                .background(Color("DividerColor"))
                .padding(.vertical, 16)
            
            VStack(spacing: 16) {
                ForEach(0..<3) { _ in
                    HStack {
                        ZStack {
                            Image("ic_housing")
                        }
                        .padding(10)
                        .background(Color("BackgroundHousing"))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Listerine")
                                .primaryText(
                                    fontName: "Manrope-SemiBold",
                                    size: 14
                                )
                            Text("12:30 · Blu by BCA")
                                .primaryText(
                                    fontName: "Manrope-Medium",
                                    size: 14,
                                    color: Color("TertiaryTextColor")
                                )
                        }
                        
                        Spacer()
                        
                        Text("- Rp30.000")
                            .primaryText(
                                fontName: "Manrope-SemiBold",
                                size: 14
                            )
                    }
                }
            }
        }
        .padding(16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(
            color: Color("ShadowColor"),
            radius: 8,
            x: 0,
            y: 2
        )
        .padding(.horizontal)
    }
}

#Preview {
    TransactionDayItem()
}
