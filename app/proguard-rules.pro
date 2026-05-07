# ── Moshi (JSON 序列化) ──
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.myorderapp.domain.model.** { *; }
-keep class com.myorderapp.data.remote.** { *; }

# ── Retrofit ──
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Koin DI ──
-keep class org.koin.** { *; }
-keep class com.myorderapp.di.** { *; }

# ── Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── ViewModel ──
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.myorderapp.ui.**ViewModel { *; }

# ── Supabase ──
-keep class io.github.jan.supabase.** { *; }

# ── Coil ──
-dontwarn coil.**

# ── SLF4J (Koin 等库引用但未使用) ──
-dontwarn org.slf4j.**

# ── 保留行号，调试用 ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
