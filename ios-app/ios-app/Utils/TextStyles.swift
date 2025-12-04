//
//  TextStyles.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 12/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct TextStylePrimary: ViewModifier {
    let textSize: CGFloat
    let textColor: Color
    let fontName: String
    
    func body(content: Content) -> some View {
        content
            .font(.custom(fontName, size: textSize))
            .foregroundColor(textColor)
    }
}

extension View {
    func primaryText(
        fontName textFontName: String = "Manrope",
        size textSize: CGFloat = 24,
        color textColor: Color = Color("PrimaryTextColor")
    ) -> some View {
        self.modifier(TextStylePrimary(textSize: textSize, textColor: textColor, fontName: textFontName))
    }
}
