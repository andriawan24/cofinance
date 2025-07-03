# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# === Performance Optimizations ===

# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# === Kotlin & Coroutines ===
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# === Android Architecture Components ===
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# === Compose ===
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Stable class * { *; }
-keep @androidx.compose.runtime.Immutable class * { *; }
-keepclassmembers class androidx.compose.** { *; }

# === Koin Dependency Injection ===
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# === Kotlinx Serialization ===
-keepattributes InnerClasses
-keep @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# === Supabase ===
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# === Google Services ===
-keep class com.google.android.** { *; }
-keep class com.google.** { *; }
-dontwarn com.google.**

# === CameraX ===
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# === Coil Image Loading ===
-keep class coil.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# === Google AI ===
-keep class dev.shreyaspatil.generativeai.** { *; }
-dontwarn dev.shreyaspatil.generativeai.**

# === Application Specific ===
-keep class id.andriawan24.cofinance.** { *; }
-keep class com.andreasgift.kmpweatherapp.BuildKonfig { *; }

# === Shared Module ===
-keep class shared.** { *; }

# === Enum classes ===
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Remove logging in release ===
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# === Remove Napier logging in release ===
-assumenosideeffects class io.github.aakira.napier.Napier {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# === Reflection optimizations ===
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# === General Android ===
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# === Parcelable ===
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# === Native methods ===
-keepclasseswithmembernames class * {
    native <methods>;
}

# === View constructors ===
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# === Remove debug info ===
-printmapping mapping.txt
-printseeds seeds.txt
-printusage usage.txt