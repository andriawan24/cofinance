//
//  BalanceCard.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 21/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct BalanceCard: View {
    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Balance")
                    .primaryText(
                        fontName: "Manrope-SemiBold",
                        size: 16,
                        color: .white
                    )
                
                HStack(spacing: 2) {
                    Text("Rp")
                        .primaryText(
                            fontName: "Manrope-Bold",
                            size: 14,
                            color: .white
                        )
                    
                    Text(60000.asFormatted)
                        .primaryText(
                            fontName: "Manrope-Bold",
                            size: 24,
                            color: .white
                        )
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color("PrimaryColor"))
            
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    IconBalanceCard(
                        image: "ic_income",
                        label: "Incomes",
                        value: 11000000
                    )
                    
                    IconBalanceCard(
                        image: "ic_income",
                        label: "Expenses",
                        value: 50000000
                    )
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.white)
        }
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

struct IconBalanceCardHorizontal: View {
    let image: String
    let label: String
    let value: Int
    
    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            Image(image)
            
            Text(label)
                .lineLimit(1)
                .primaryText(
                    fontName: "Manrope-SemiBold",
                    size: 12,
                    color: Color("SecondaryTextColor")
                )
            
            Spacer()
            
            Text(value.asRupiah)
                .lineLimit(1)
                .primaryText(
                    fontName: "Manrope-SemiBold",
                    size: 14
                )
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
    }
}

struct IconBalanceCard: View {
    let image: String
    let label: String
    let value: Int
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(image)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .lineLimit(1)
                    .primaryText(
                        fontName: "Manrope-SemiBold",
                        size: 12,
                        color: Color("SecondaryTextColor")
                    )
                
                Text(value.asRupiah)
                    .lineLimit(1)
                    .primaryText(
                        fontName: "Manrope-SemiBold",
                        size: 16
                    )
            }
        }
        .frame(maxWidth: .infinity)
        .padding(8)
        .background(Color("BackgroundGrey"))
        .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

#Preview {
    BalanceCard()
}
