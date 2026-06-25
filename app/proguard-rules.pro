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

# ── Supabase / Ktor ──
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ── Coil 3 ──
-dontwarn coil3.**

# ── Lottie ──
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** {*;}

# ── Retrofit extra ──
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# ── OkHttp extra ──
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── 保留行号，调试用 ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
