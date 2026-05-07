package com.myorderapp.di

import android.content.Context
import com.myorderapp.ApiConfig
import com.myorderapp.data.remote.recipe.JuheRecipeApi
import com.myorderapp.data.remote.recipe.SpoonacularApi
import com.myorderapp.data.remote.recipe.TheMealDBApi
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
const val SPOONACULAR_BASE_URL = "https://api.spoonacular.com/"
const val THEMEALDB_BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

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

    // Spoonacular Retrofit
    single(named("spoonacular")) {
        Retrofit.Builder()
            .baseUrl(SPOONACULAR_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>(named("spoonacular")).create(SpoonacularApi::class.java) }

    // TheMealDB Retrofit
    single(named("themealdb")) {
        Retrofit.Builder()
            .baseUrl(THEMEALDB_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>(named("themealdb")).create(TheMealDBApi::class.java) }

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
