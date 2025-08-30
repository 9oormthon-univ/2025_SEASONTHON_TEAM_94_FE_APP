# Flutter wrapper
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.**  { *; }
-keep class io.flutter.util.**  { *; }
-keep class io.flutter.view.**  { *; }
-keep class io.flutter.**  { *; }
-keep class io.flutter.plugins.**  { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.support.**

# Google Auto Value (for TensorFlow Lite Support)
-dontwarn com.google.auto.value.**

# Google Play Core (Flutter uses it but we don't need it)
-dontwarn com.google.android.play.core.**

# Notification Listener Service
-keep class kr.klr.stopusing.NotificationListenerService { *; }
-keep class kr.klr.stopusing.AITransactionParser { *; }
-keep class kr.klr.stopusing.MainActivity { *; }

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# SQLite
-keep class androidx.sqlite.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}