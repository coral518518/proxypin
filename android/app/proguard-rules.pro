#Flutter Wrapper
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.**  { *; }
-keep class io.flutter.util.**  { *; }
-keep class io.flutter.view.**  { *; }
-keep class io.flutter.**  { *; }
-keep class io.flutter.plugins.**  { *; }
-keep class de.prosiebensat1digital.** { *; }

-dontwarn com.google.android.play.core.**

# tun2proxy JNI bindings
-keep class com.github.shadowsocks.bg.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
