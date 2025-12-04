//
//  SlimePageIndicator.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 12/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SlimePageIndicator: View {
    @Binding var currentPage: Int
    let numberOfPages: Int
    private let config: Config = Config()
    
    private var adjustedIndex: Int {
        return currentPage < 0 ? numberOfPages : (currentPage > numberOfPages ? 0 : currentPage)
    }
    
    struct Config {
        var dotSize: CGFloat = 4
        var pageIndicatorHighlight: Color = Color("PrimaryColor")
        var pageIndicatorNext: Color = Color("PrimaryColor")
        var pageIndicatorLast: Color = Color("PrimaryColor")
    }
    
    var body: some View {
        if (numberOfPages > 0) {
            HStack(spacing: 2) {
                ForEach(Array(0..<numberOfPages), id: \.self) { index in
                    Capsule()
                        .fill(
                            index == adjustedIndex ? config.pageIndicatorHighlight : index < currentPage ? config.pageIndicatorLast : config.pageIndicatorNext)
                        .frame(width: index == currentPage ? config.dotSize * 8 : config.dotSize, height: config.dotSize)
                        .transition(.slide)
                        .animation(.easeInOut, value: currentPage)
                }
            }
        }
    }
}
