# AdMore SDK ProGuard Rules

# Keep SDK entry points
-keep public class com.seamlabs.admore.AdMoreSDK { *; }
-keep public class com.seamlabs.admore.presentation.callback.** { *; }

# Keep Dagger components and modules
-keep class com.seamlabs.admore.di.** { *; }
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}
-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.internal.Factory

# Keep models
-keep class com.seamlabs.admore.domain.model.** { *; }
-keep class com.seamlabs.admore.data.model.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Retrofit and OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson rules
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers enum * { *; }

# Android X
-dontwarn androidx.**
-keep class androidx.** { *; }

# AdMore SDK specific
-keepclassmembers class com.seamlabs.admore.data.source.local.collector.** { *; }
-keepclassmembers enum com.seamlabs.admore.** { *; }

# Keep annotation attributes used for DI
-keepattributes RuntimeVisibleAnnotations
-keepattributes AnnotationDefault