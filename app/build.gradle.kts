import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.myorderapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.myorderapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 30
        versionName = "1.4.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "JUHE_API_KEY", "\"${localProps.getProperty("JUHE_API_KEY", "")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"${localProps.getProperty("SPOONACULAR_API_KEY", "")}\"")
        buildConfigField("String", "TIAN_API_KEY", "\"${localProps.getProperty("TIAN_API_KEY", "")}\"")
        buildConfigField("String", "JISU_API_KEY", "\"${localProps.getProperty("JISU_API_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../orderdisk.jks")
            storePassword = "orderdisk123"
            keyAlias = "orderdisk"
            keyPassword = "orderdisk123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Retrofit + OkHttp + Moshi
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)

    // Supabase
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.supabase.realtime.kt)
    implementation(libs.supabase.storage.kt)
    implementation(libs.supabase.auth.kt)
    implementation(libs.supabase.compose.auth)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.room.paging)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // WorkManager (upload queue)
    implementation(libs.work.runtime.ktx)

    // Animations
    implementation(libs.lottie.compose)

    // Accompanist
    // Accompanist removed — use Compose Foundation built-in permissions API

    // Koin DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
