//
//  NumberHelper.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 21/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

extension Numeric {
    var asRupiah: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "id_ID")
        formatter.maximumFractionDigits = 0
        return formatter.string(for: self) ?? ""
    }
    
    var asFormatted: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "id_ID")
        formatter.maximumFractionDigits = 0
        return formatter.string(for: self) ?? ""
    }
}
