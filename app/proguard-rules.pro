# ProGuard rules for the release build.
# R8 does dead-code elimination + obfuscation; rules below only target
# libraries whose metadata is read reflectively at runtime.

# Kotlinx Serialization — @Serializable classes are referenced by name.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class app.calsnap.android.**$$serializer { *; }
-keepclassmembers class app.calsnap.android.** {
    *** Companion;
}
-keepclasseswithmembers class app.calsnap.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt / Dagger generates classes with specific names; keep reflection-safe.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room — keep entity constructors for the Room compiler's generated code.
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# OkHttp + Retrofit need some platform reflection to pick conscrypt/etc.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keep class retrofit2.** { *; }

# Keep Compose tooling metadata out of release — minor size win.
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(androidx.compose.runtime.Composer, java.lang.String);
    void sourceInformationMarkerStart(androidx.compose.runtime.Composer, int, java.lang.String);
    void sourceInformationMarkerEnd(androidx.compose.runtime.Composer);
}
