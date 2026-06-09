# Add project specific ProGuard rules here.

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep native library
-keep class comkuaihuiai.native.** { *; }

# Keep data classes
-keep class comkuaihuiai.data.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Compose
-dontwarn androidx.compose.**
