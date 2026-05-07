package com.myorderapp.di

import android.content.Context
import com.myorderapp.ApiConfig
import com.myorderapp.data.remote.recipe.JuheRecipeApi
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.data.remote.supabase.SupabaseAuthApi
import com.myorderapp.data.remote.supabase.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

const val JUHE_BASE_URL = "https://apis.juhe.cn/"

val networkModule = module {

    single {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Session Manager (with persistent storage)
    single { SessionManager(get<Context>()) }

    // Juhe Retrofit
    single(named("juhe")) {
        Retrofit.Builder()
            .baseUrl(JUHE_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>(named("juhe")).create(JuheRecipeApi::class.java) }

    // Supabase Retrofit
    single(named("supabase")) {
        Retrofit.Builder()
            .baseUrl(ApiConfig.SUPABASE_URL + "/rest/v1/")
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>(named("supabase")).create(SupabaseApi::class.java) }

    // Supabase Auth Retrofit
    single(named("supabase_auth")) {
        Retrofit.Builder()
            .baseUrl(ApiConfig.SUPABASE_URL + "/")
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>(named("supabase_auth")).create(SupabaseAuthApi::class.java) }
}
