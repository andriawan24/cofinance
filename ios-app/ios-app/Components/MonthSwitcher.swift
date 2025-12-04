//
//  MonthSwitcher.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 30/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct MonthSwitcher: View {
    var body: some View {
        HStack {
            Button {
                // TODO: Implement previous button
            } label: {
                Image(systemName: "chevron.left")
                    .foregroundStyle(.white)
                    .fontWeight(.semibold)
            }
            .buttonStyle(.plain)
            
            Spacer()
            
            Text("May 2025")
                .primaryText(fontName: "Manrope-Bold", size: 16, color: .white)
            
            Spacer()
            
            Button {
                // TODO: Implement next button
            } label: {
                Image(systemName: "chevron.right")
                    .foregroundStyle(.white)
                    .fontWeight(.semibold)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20)
        .padding(.vertical)
        .background {
            Capsule()
                .fill(Color("PrimaryColor"))
        }
        .padding(.horizontal)
    }
}

struct MonthSwitcherStats: View {
    var body: some View {
        HStack {
            Button {
                // TODO: Implement previous button
            } label: {
                Image(systemName: "chevron.left")
                    .foregroundStyle(Color("PrimaryColor"))
                    .fontWeight(.semibold)
            }
            .buttonStyle(.plain)
            
            Spacer()
            
            Text("May 2025")
                .primaryText(
                    fontName: "Manrope-SemiBold",
                    size: 16
                )
            
            Spacer()
            
            Button {
                // TODO: Implement next button
            } label: {
                Image(systemName: "chevron.right")
                    .foregroundStyle(Color("PrimaryColor"))
                    .fontWeight(.semibold)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20)
        .padding(.vertical)
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(.white)
        }
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
    MonthSwitcherStats()
}
