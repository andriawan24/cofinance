//
//  KotlinExtensions.swift
//  ios-app
//
//  Created by Claude Code
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import shared

// Extension to check if Kotlin Result is success
extension NSObject {
    var isSuccess: Bool {
        let mirror = Mirror(reflecting: self)
        for child in mirror.children {
            if child.label == "isSuccess" {
                return child.value as? Bool ?? false
            }
        }
        // Fallback: check if there's a "value" property (success case)
        return mirror.children.contains { $0.label == "value" }
    }

    var isFailure: Bool {
        return !isSuccess
    }
}