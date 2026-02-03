# feat: Implement Google Sign-In for iOS using Native SDK

**Date:** 2025-02-03
**Status:** Implemented
**Type:** Enhancement
**Priority:** High

---

## Overview

Implement Google Sign-In functionality for iOS in the Cofinance Kotlin Multiplatform project. The current ComposeAuth `googleNativeLogin` configuration is not working on iOS because it lacks the native iOS implementation. This plan implements Google Sign-In using the native Google Sign-In iOS SDK through Kotlin/Swift interop.

---

## Problem Statement / Motivation

**Current State:**
- Android Google Sign-In works via Credential Manager (`composeApp/src/androidMain/`)
- iOS uses `rememberSignInWithGoogle` composable but native implementation is missing
- Users on iOS cannot sign in with Google, blocking a significant authentication pathway

**Why This Matters:**
- Google Sign-In is a primary authentication method for the app
- iOS users represent a significant portion of the user base
- Without this, iOS users must use alternative auth methods (if available)

**Technical Constraint:**
- Google explicitly blocks OAuth from WKWebView/UIWebView since April 2017
- Must use ASWebAuthenticationSession, Google Sign-In iOS SDK, or AppAuth-iOS

---

## Proposed Solution

Implement Google Sign-In for iOS using the **Google Sign-In iOS SDK** integrated via CocoaPods/Swift Package Manager. This approach:

1. Follows Google's official recommendation
2. Provides the best user experience (native UI, account picker)
3. Works with the existing Supabase Auth backend
4. Uses the established expect/actual pattern already in the codebase

### Architecture Decision

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| **Google Sign-In iOS SDK** | Official SDK, best UX, maintained by Google | Requires Swift interop | **Recommended** |
| ASWebAuthenticationSession | Native iOS, minimal dependencies | More complex token handling | Alternative |
| AppAuth-iOS | Standard OAuth, flexible | More configuration, larger footprint | Not recommended |

**Rationale:** The Google Sign-In iOS SDK is Google's official solution, provides the best user experience with native account picker, and handles token management internally. The project already has iOS interop patterns established (see `PlatformSettings.ios.kt`).

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        commonMain                                │
├─────────────────────────────────────────────────────────────────┤
│  LoginScreen.kt                                                  │
│  ├── rememberSignInWithGoogle() ─────────────┐                  │
│  └── authState.startFlow() triggers auth     │                  │
│                                               │                  │
│  GoogleAuthManager (expect interface)         │                  │
│  ├── signIn(): Result<GoogleAuthResult>      │                  │
│  └── signOut()                                │                  │
└───────────────────────────────────────────────┼──────────────────┘
                                                │
        ┌───────────────────────────────────────┼───────────────────┐
        │                                       │                   │
        ▼                                       ▼                   ▼
┌───────────────────┐  ┌────────────────────────────┐  ┌───────────────┐
│   androidMain     │  │         iosMain            │  │  desktopMain  │
├───────────────────┤  ├────────────────────────────┤  ├───────────────┤
│ Credential Mgr    │  │ GoogleSignInHelper.kt      │  │ Stub impl     │
│ (existing)        │  │ ├── Uses native iOS SDK    │  │ (OAuth flow)  │
└───────────────────┘  │ └── via Swift interop      │  └───────────────┘
                       └────────────────────────────┘
                                    │
                                    ▼
                       ┌────────────────────────────┐
                       │     Swift Layer            │
                       │  GoogleSignInBridge.swift  │
                       │  ├── GIDSignIn SDK calls   │
                       │  └── Callback to Kotlin    │
                       └────────────────────────────┘
                                    │
                                    ▼
                       ┌────────────────────────────┐
                       │  Google Sign-In iOS SDK    │
                       │  (via Swift Package Mgr)   │
                       └────────────────────────────┘
```

### Data Flow

```
User taps "Sign in with Google"
         │
         ▼
┌─────────────────────────────────────┐
│  1. LoginScreen.kt                  │
│     authState.startFlow()           │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  2. GoogleAuthManager.signIn()      │
│     (expect/actual)                 │
└──────────────┬──────────────────────┘
               │
               ▼ (iOS)
┌─────────────────────────────────────┐
│  3. GoogleSignInHelper.kt           │
│     Calls Swift bridge              │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  4. GoogleSignInBridge.swift        │
│     GIDSignIn.sharedInstance.signIn │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  5. Google Sign-In UI               │
│     (native account picker)         │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  6. Callback with ID Token          │
│     → GoogleSignInBridge            │
│     → GoogleSignInHelper            │
│     → LoginScreen                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  7. Supabase IDToken Exchange       │
│     supabase.auth.signInWith(IDToken)│
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  8. Navigate to Home                │
└─────────────────────────────────────┘
```

---

## Implementation Phases

### Phase 1: Foundation - iOS SDK Integration

**Tasks:**
- [ ] Add Google Sign-In iOS SDK dependency via Swift Package Manager
- [ ] Configure `Info.plist` with required URL schemes and client IDs
- [ ] Create Swift bridge class `GoogleSignInBridge.swift`
- [ ] Implement `AppDelegate` URL handling for OAuth callback

**Files to Create:**
- `iosApp/iosApp/GoogleSignInBridge.swift`

**Files to Modify:**
- `iosApp/iosApp/Info.plist` (already has partial config)
- `iosApp/iosApp/iOSApp.swift` (add AppDelegate)

### Phase 2: Kotlin Interop Layer

**Tasks:**
- [ ] Create `GoogleAuthManager` expect/actual interface in commonMain
- [ ] Implement iOS actual using interop with Swift bridge
- [ ] Create `GoogleAuthResult` sealed class for success/error/cancel states
- [ ] Update existing `rememberSignInWithGoogle` to use new manager

**Files to Create:**
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthManager.kt`
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthResult.kt`
- `composeApp/src/iosMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthManager.ios.kt`
- `composeApp/src/androidMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthManager.android.kt`
- `composeApp/src/desktopMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthManager.desktop.kt`

### Phase 3: Supabase Integration

**Tasks:**
- [ ] Verify Supabase Google provider configuration
- [ ] Enable "Skip nonce check" in Supabase dashboard (required for iOS)
- [ ] Implement token exchange with Supabase Auth
- [ ] Handle error scenarios (invalid token, network errors)

**Files to Modify:**
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/pages/login/LoginScreen.kt`
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/datasource/SupabaseDataSource.kt`

### Phase 4: Error Handling & UX

**Tasks:**
- [ ] Implement comprehensive error handling
- [ ] Add localized error messages to strings.xml
- [ ] Create loading states during sign-in flow
- [ ] Handle user cancellation gracefully

**Files to Modify:**
- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/composeResources/values-id/strings.xml`

---

## Acceptance Criteria

### Functional Requirements
- [ ] User can tap "Sign in with Google" on iOS and see Google account picker
- [ ] User can select Google account and complete authentication
- [ ] User is navigated to home screen after successful sign-in
- [ ] User can cancel sign-in and return to login screen
- [ ] Error messages are shown for network failures and other errors
- [ ] Sign-in persists across app restarts (Supabase session)

### Non-Functional Requirements
- [ ] Sign-in flow completes within 5 seconds (excluding user interaction)
- [ ] No sensitive data (tokens) logged to console in release builds
- [ ] Follows Google Sign-In branding guidelines
- [ ] Works on iOS 12+ (ASWebAuthenticationSession minimum)

### Quality Gates
- [ ] All existing tests continue to pass
- [ ] Manual testing on physical iOS device
- [ ] Manual testing on iOS Simulator
- [ ] Code reviewed for security vulnerabilities

---

## Dependencies & Prerequisites

### External Dependencies
- Google Sign-In iOS SDK (v9.0.0+)
- Supabase Google provider enabled
- Google Cloud Console OAuth client configured

### Internal Dependencies
- Existing `SupabaseDataSource.kt:30-48` IDToken login method
- Existing `LoginScreen.kt:22-38` sign-in composable
- Existing expect/actual pattern in `PlatformSettings.kt`

### Configuration Required
1. **Google Cloud Console:**
   - iOS OAuth client ID (already exists: `441243986243-gaq25a4d4nris488iuokh4pfqqtop4if.apps.googleusercontent.com`)
   - Web client ID for server (already exists in BuildKonfig)

2. **Supabase Dashboard:**
   - Enable Google provider
   - Add iOS client ID
   - Enable "Skip nonce check" option

3. **iOS Project:**
   - `Info.plist` URL schemes (partially configured)
   - GoogleSignIn SPM dependency

---

## Risk Analysis & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Google SDK update breaks interop | Low | High | Pin SDK version, monitor release notes |
| Swift/Kotlin interop complexity | Medium | Medium | Use simple callback pattern, avoid complex types |
| Supabase nonce validation fails | Medium | High | Enable "Skip nonce check" in Supabase |
| User has no Google account on device | Low | Low | Show appropriate error message |
| Network timeout during token exchange | Medium | Medium | Implement retry with exponential backoff |

---

## Security Considerations

1. **Token Storage:** Supabase SDK handles token storage in iOS Keychain
2. **Nonce Handling:** Will use "Skip nonce check" option (Google iOS SDK limitation)
3. **URL Scheme:** Custom scheme registered in Info.plist prevents hijacking
4. **No Client Secret:** iOS apps use public client ID only (no secret)
5. **SSL Pinning:** Ktor client uses system SSL for Supabase calls

---

## MVP Implementation

### GoogleAuthManager.kt (commonMain)

```kotlin
// composeApp/src/commonMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthManager.kt

package id.andriawan.cofinance.auth

sealed class GoogleAuthResult {
    data class Success(val idToken: String, val email: String?) : GoogleAuthResult()
    data class Error(val message: String, val exception: Exception? = null) : GoogleAuthResult()
    data object Cancelled : GoogleAuthResult()
}

expect class GoogleAuthManager() {
    suspend fun signIn(): GoogleAuthResult
    fun signOut()
}
```

### GoogleAuthManager.ios.kt (iosMain)

```kotlin
// composeApp/src/iosMain/kotlin/id/andriawan/cofinance/auth/GoogleAuthManager.ios.kt

package id.andriawan.cofinance.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class GoogleAuthManager {

    actual suspend fun signIn(): GoogleAuthResult = suspendCancellableCoroutine { continuation ->
        // Call Swift bridge via interop
        GoogleSignInBridgeKt.signIn { idToken, email, error, cancelled ->
            when {
                cancelled -> continuation.resume(GoogleAuthResult.Cancelled)
                error != null -> continuation.resume(
                    GoogleAuthResult.Error(error.localizedDescription ?: "Unknown error")
                )
                idToken != null -> continuation.resume(
                    GoogleAuthResult.Success(idToken, email)
                )
                else -> continuation.resume(
                    GoogleAuthResult.Error("No token received")
                )
            }
        }
    }

    actual fun signOut() {
        GoogleSignInBridgeKt.signOut()
    }
}
```

### GoogleSignInBridge.swift (iosApp)

```swift
// iosApp/iosApp/GoogleSignInBridge.swift

import Foundation
import GoogleSignIn

@objc public class GoogleSignInBridge: NSObject {

    @objc public static let shared = GoogleSignInBridge()

    @objc public func signIn(
        completion: @escaping (String?, String?, NSError?, Bool) -> Void
    ) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            completion(nil, nil, NSError(domain: "GoogleSignIn", code: -1,
                userInfo: [NSLocalizedDescriptionKey: "No root view controller"]), false)
            return
        }

        // Get client ID from Info.plist
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String else {
            completion(nil, nil, NSError(domain: "GoogleSignIn", code: -2,
                userInfo: [NSLocalizedDescriptionKey: "Missing GIDClientID in Info.plist"]), false)
            return
        }

        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
            if let error = error as? NSError {
                // Check if user cancelled
                if error.code == GIDSignInError.canceled.rawValue {
                    completion(nil, nil, nil, true)
                } else {
                    completion(nil, nil, error, false)
                }
                return
            }

            guard let user = result?.user,
                  let idToken = user.idToken?.tokenString else {
                completion(nil, nil, NSError(domain: "GoogleSignIn", code: -3,
                    userInfo: [NSLocalizedDescriptionKey: "No ID token"]), false)
                return
            }

            let email = user.profile?.email
            completion(idToken, email, nil, false)
        }
    }

    @objc public func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    @objc public func handleURL(_ url: URL) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}
```

### Updated LoginScreen.kt

```kotlin
// Updates to composeApp/src/commonMain/kotlin/id/andriawan/cofinance/pages/login/LoginScreen.kt

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val googleAuthManager = remember { GoogleAuthManager() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ... existing UI code ...

    OutlinedButton(
        enabled = !isLoading,
        onClick = {
            scope.launch {
                isLoading = true
                errorMessage = null

                when (val result = googleAuthManager.signIn()) {
                    is GoogleAuthResult.Success -> {
                        // Exchange with Supabase
                        try {
                            supabase.auth.signInWith(IDToken) {
                                idToken = result.idToken
                                provider = Google
                            }
                            onNavigateToHome()
                        } catch (e: Exception) {
                            errorMessage = "Failed to complete sign-in. Please try again."
                        }
                    }
                    is GoogleAuthResult.Error -> {
                        errorMessage = result.message
                    }
                    is GoogleAuthResult.Cancelled -> {
                        // User cancelled, no action needed
                    }
                }

                isLoading = false
            }
        }
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Text(stringResource(Res.string.sign_in_with_google))
        }
    }

    errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

---

## References & Research

### Internal References
- Authentication setup: `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/utils/SupabaseHelper.kt:22-24`
- Login flow: `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/pages/login/LoginScreen.kt:22-38`
- IDToken login: `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/datasource/SupabaseDataSource.kt:39-44`
- expect/actual pattern: `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/PlatformSettings.kt:1-16`
- iOS permissions: `composeApp/src/iosMain/kotlin/id/andriawan/cofinance/PlatformSettings.ios.kt:1-42`
- iOS Info.plist: `iosApp/iosApp/Info.plist:13-25`

### External References
- [Google Sign-In iOS SDK](https://developers.google.com/identity/sign-in/ios/start-integrating)
- [Supabase Google Auth](https://supabase.com/docs/guides/auth/social-login/auth-google)
- [Kotlin/Native Swift Interop](https://kotlinlang.org/docs/native-objc-interop.html)
- [KMP Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

### Related Work
- Google blocks WebView OAuth: [Google Security Announcement](https://developers.googleblog.com/upcoming-security-changes-to-googles-oauth-20-authorization-endpoint-in-embedded-webviews/)
- ComposeAuth Plugin: [supabase-kt-plugins](https://github.com/supabase-community/supabase-kt-plugins)
- KMPAuth Library: [mirzemehdi/KMPAuth](https://github.com/mirzemehdi/KMPAuth)

---

## Open Questions

1. **Server Client ID:** Verify the server client ID in BuildKonfig is correct for Supabase token exchange
2. **Account Linking:** What should happen if user signs in with Google using email that already has password auth?
3. **Sign Out:** Should Google sign-out also sign out from Supabase, or just the Google session?

---

## Appendix: Info.plist Updates

```xml
<!-- Already present, verify correctness -->
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.441243986243-gaq25a4d4nris488iuokh4pfqqtop4if</string>
        </array>
    </dict>
</array>
<key>GIDClientID</key>
<string>441243986243-gaq25a4d4nris488iuokh4pfqqtop4if.apps.googleusercontent.com</string>

<!-- May need to add server client ID -->
<key>GIDServerClientID</key>
<string>YOUR_WEB_CLIENT_ID</string>
