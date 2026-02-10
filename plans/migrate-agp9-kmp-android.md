# feat: Migrate to `com.android.kotlin.multiplatform.library` for AGP 9.0

**Type:** refactor
**Risk:** HIGH - structural change, all-or-nothing migration
**Estimated scope:** ~15 files touched, 1 new module created

---

## Overview

AGP 9.0.0 deprecates using `com.android.application` alongside `org.jetbrains.kotlin.multiplatform` in the same module. The project must split into two modules:

- **`composeApp`** (KMP shared library) - uses `com.android.kotlin.multiplatform.library`
- **`androidApp`** (Android application entry point) - uses `com.android.application`

The `com.android.kotlin.multiplatform.library` plugin is **library-only** - there is no application variant. This means the Android application entry point (MainActivity, app manifest, launcher icons, build types, signing) must live in a separate module.

**Deadline:** AGP 10.0 (H2 2026) will remove the legacy APIs entirely. Current `gradle.properties` flags (`android.newDsl=false`, `android.builtInKotlin=false`) are keeping the old behavior alive temporarily.

---

## Current Architecture (Single Module)

```
Cofinance_Shared/
  composeApp/                          # Does EVERYTHING
    build.gradle.kts                   # com.android.application + kotlin.multiplatform
    src/
      commonMain/                      # ~90 shared Kotlin files + composeResources/
      androidMain/                     # MainActivity + actual impls + AndroidManifest.xml + res/
      iosMain/                         # 6 actual impl files
      desktopMain/                     # 6 actual impl files
      webMain/                         # 3 actual impl files (shared JS+WasmJS)
      jsMain/                          # 3 actual impl files
      wasmJsMain/                      # 3 actual impl files
```

## Target Architecture (Two Modules)

```
Cofinance_Shared/
  composeApp/                          # KMP shared library
    build.gradle.kts                   # kotlin.multiplatform + com.android.kotlin.multiplatform.library
    src/
      commonMain/                      # UNCHANGED
      androidMain/                     # actual impls ONLY (no MainActivity, no app manifest)
      iosMain/                         # UNCHANGED
      desktopMain/                     # UNCHANGED
      webMain/                         # UNCHANGED
      jsMain/                          # UNCHANGED
      wasmJsMain/                      # UNCHANGED
  androidApp/                          # NEW: Android application entry point
    build.gradle.kts                   # com.android.application
    src/main/
      AndroidManifest.xml              # MOVED: full app manifest
      kotlin/.../MainActivity.kt       # MOVED from composeApp/androidMain
      res/                             # MOVED: mipmaps, strings.xml
```

---

## Acceptance Criteria

- [ ] Gradle sync succeeds with no deprecation warnings about `androidTarget()`
- [ ] `./gradlew :androidApp:assembleDebug` builds successfully
- [ ] `./gradlew :androidApp:assembleRelease` builds successfully
- [ ] Android app launches, Google Sign-In works, Camera works
- [ ] iOS, Desktop, JS, WasmJS targets compile without changes
- [ ] BuildKonfig generates config fields accessible from all platforms
- [ ] Compose Multiplatform resources load correctly on Android
- [ ] `gradle.properties` no longer uses legacy compatibility flags

---

## Implementation Plan

### Phase 1: Create `androidApp` module

#### 1.1 Create directory structure

```
mkdir -p androidApp/src/main/kotlin/id/andriawan/cofinance
mkdir -p androidApp/src/main/res
```

#### 1.2 Create `androidApp/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "id.andriawan.cofinance"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.andriawan.cofinance"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    debugImplementation(compose.uiTooling)
}
```

**Notes:**
- `proguardFiles` removed since `proguard-rules.pro` doesn't exist on disk
- AGP 9.0 has built-in Kotlin support, no separate `kotlin-android` plugin needed
- `debugImplementation(compose.uiTooling)` works here because app module has build variants
- `compose.uiTooling` accessor requires the CMP plugin - use explicit coordinates instead: `debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.10.0")` or add the CMP plugin

#### 1.3 Move files from `composeApp/src/androidMain/` to `androidApp/src/main/`

| Source | Destination |
|--------|------------|
| `composeApp/src/androidMain/kotlin/.../MainActivity.kt` | `androidApp/src/main/kotlin/id/andriawan/cofinance/MainActivity.kt` |
| `composeApp/src/androidMain/AndroidManifest.xml` | `androidApp/src/main/AndroidManifest.xml` |
| `composeApp/src/androidMain/res/mipmap-*/**` | `androidApp/src/main/res/mipmap-*/**` |
| `composeApp/src/androidMain/res/values/strings.xml` | `androidApp/src/main/res/values/strings.xml` |
| `composeApp/src/androidMain/res/resources.properties` | `androidApp/src/main/res/resources.properties` |

**Files that STAY in `composeApp/src/androidMain/`:**
- `kotlin/.../utils/Helper.android.kt` (expect/actual)
- `kotlin/.../PlatformSettings.android.kt` (expect/actual)
- `kotlin/.../auth/GoogleAuthManager.android.kt` (expect/actual)
- `kotlin/.../components/CameraPreviewContent.android.kt` (expect/actual)
- `kotlin/.../localization/AppLocaleManager.android.kt` (expect/actual)

#### 1.4 Create library AndroidManifest for `composeApp`

After moving the full manifest to `androidApp`, create a minimal library manifest at `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

</manifest>
```

The app manifest (`androidApp/src/main/AndroidManifest.xml`) keeps the `<application>` block with activities:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name="io.github.jan.supabase.compose.auth.ComposeAuthHandler"
            android:exported="true">
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="https"
                    android:host="jxufescyhwtoocyvnerp.supabase.co"
                    android:path="/auth/v1/callback" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Note:** The `ComposeAuthHandler` activity from Supabase stays in the app manifest since it's an exported activity with intent filters. Android manifest merger will combine library permissions + app activities at build time.

#### 1.5 Update `settings.gradle.kts`

Add `:androidApp` module:

```kotlin
include(":composeApp")
include(":androidApp")
```

---

### Phase 2: Convert `composeApp` to KMP library

#### 2.1 Update `composeApp/build.gradle.kts` plugins

**Before:**
```kotlin
plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.cmp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)    // REMOVE
    alias(libs.plugins.kotlinSerialization)
    id("com.codingfeline.buildkonfig")
}
```

**After:**
```kotlin
plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.cmp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)     // REPLACE
    alias(libs.plugins.kotlinSerialization)
    id("com.codingfeline.buildkonfig")
}
```

#### 2.2 Replace `androidTarget {}` + `android {}` with `androidLibrary {}`

**Before:**
```kotlin
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    // ... other targets
}

android {
    namespace = "id.andriawan.cofinance"
    compileSdk = 36
    defaultConfig { ... }
    packaging { ... }
    buildTypes { ... }
    compileOptions { ... }
    androidResources { ... }
}
```

**After:**
```kotlin
kotlin {
    androidLibrary {
        namespace = "id.andriawan.cofinance.shared"
        compileSdk = 36
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        androidResources.enable = true
    }
    // ... other targets UNCHANGED
}

// REMOVE the entire top-level android {} block
```

**Key changes:**
- Namespace changes to `id.andriawan.cofinance.shared` (must differ from app module)
- `androidResources.enable = true` is **required** for Compose Multiplatform resources to work
- No `applicationId`, `targetSdk`, `versionCode`, `versionName` (library, not app)
- No `buildTypes`, `packaging`, `compileOptions` blocks (moved to `androidApp`)
- `minSdk` is set directly (no `defaultConfig` wrapper)

#### 2.3 Update `androidMain.dependencies`

**Before:**
```kotlin
androidMain.dependencies {
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)       // MOVE to androidApp
    implementation(libs.androidx.appcompat)               // MOVE to androidApp
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.camerak)
}
```

**After:**
```kotlin
androidMain.dependencies {
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.camerak)
}
```

`androidx.activity.compose` and `androidx.appcompat` move to `androidApp/build.gradle.kts` since they're only needed by the Activity host.

#### 2.4 Remove top-level `dependencies` block

**Before:**
```kotlin
dependencies {
    debugImplementation(compose.uiTooling)
}
```

**After:** Remove entirely. `debugImplementation` doesn't work in single-variant library. The UI tooling dependency is handled in `androidApp` instead.

---

### Phase 3: Update `gradle.properties`

**Changes:**

| Property | Before | After | Reason |
|----------|--------|-------|--------|
| `android.builtInKotlin` | `false` | Remove line | AGP 9.0 default is `true`, needed for built-in Kotlin |
| `android.newDsl` | `false` | Remove line | AGP 9.0 default is `true`, needed for new plugin |
| `android.uniquePackageNames` | `false` | `true` | Two modules now need unique namespaces |

All other properties can remain as-is.

---

### Phase 4: Update BuildKonfig

BuildKonfig stays in `composeApp` (confirmed compatible with `com.android.kotlin.multiplatform.library` per BuildKonfig 0.17.1). The `buildkonfig` block in `composeApp/build.gradle.kts` remains unchanged:

```kotlin
buildkonfig {
    packageName = "com.andriawan.cofinance"    // unchanged
    defaultConfigs { ... }                      // unchanged
}
```

Generated config classes will be accessible from `androidApp` transitively through the `implementation(project(":composeApp"))` dependency.

---

## Verification Checklist

After implementation, verify:

- [ ] `./gradlew :composeApp:build` - KMP library builds for all targets
- [ ] `./gradlew :androidApp:assembleDebug` - Android debug APK builds
- [ ] `./gradlew :androidApp:assembleRelease` - Android release APK builds
- [ ] Install and launch on device - MainActivity starts, `App()` composable renders
- [ ] Google Sign-In flow works (credentials APIs in shared `androidMain`, Activity in `androidApp`)
- [ ] Camera capture works (CameraK in shared `androidMain`)
- [ ] Image preview and scan receipt flow works
- [ ] Compose resources (fonts, drawables, strings from `composeResources/`) load on Android
- [ ] `./gradlew :composeApp:iosArm64MainKlibrary` - iOS still compiles
- [ ] `./gradlew :composeApp:desktopJar` - Desktop still compiles
- [ ] `./gradlew :composeApp:jsBrowserDistribution` - JS still compiles
- [ ] `./gradlew :composeApp:wasmJsBrowserDistribution` - WasmJS still compiles
- [ ] No deprecation warnings about `androidTarget()`

---

## Risk Mitigation

1. **Create this on a separate branch** - easy rollback if migration fails
2. **Temporary fallback** - if blocked, add `android.enableLegacyVariantApi=true` to `gradle.properties` to defer migration (works until AGP 10.0)
3. **Context injection** - `GoogleAuthManager` and camera code in `androidMain` use `PlatformContext` (Coil's `PlatformContext` wraps Android `Context`), which is provided by Compose's `LocalPlatformContext`. Since `MainActivity` calls `App()` composable from the shared module, the Activity context flows through Compose's composition - **no refactoring needed**
4. **Supabase ComposeAuthHandler** - declared in app manifest, resolved at runtime through manifest merger. The Supabase library registers this activity class, so it works regardless of which module declares it in the manifest

---

## References

- [Android Developer Docs: Set up the Android Gradle Library Plugin for KMP](https://developer.android.com/kotlin/multiplatform/plugin)
- [Kotlin Docs: Updating multiplatform projects with Android apps to use AGP 9](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)
- [JetBrains Blog: Update your Kotlin projects for AGP 9.0](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/)
- [AGP 9.0.0 Release Notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [BuildKonfig #261: AGP 9.0 compatibility confirmed](https://github.com/yshrsmz/BuildKonfig/issues/261)
- [Reference project: KMP Wizard Template for AGP 9](https://github.com/watermelonKode/kmp-wizard-template)
- [Reference project: get-started-with-cm (new-project-structure branch)](https://github.com/kotlin-hands-on/get-started-with-cm/tree/new-project-structure)
- [AGP 9 Migration Guide (nek12.dev)](https://nek12.dev/blog/en/agp-9-0-migration-guide-android-gradle-plugin-9-kmp-migration-kotlin)
