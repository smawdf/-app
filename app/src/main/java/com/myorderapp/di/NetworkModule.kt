package com.myorderapp.di

import com.myorderapp.ApiConfig
import com.myorderapp.data.remote.recipe.SpoonacularApi
import com.myorderapp.data.remote.recipe.SpoonacularExternalDishImageSource
import com.myorderapp.data.remote.recipe.TheMealDbApi
import com.myorderapp.data.remote.recipe.TheMealDbExternalDishImageSource
import com.myorderapp.data.remote.recipe.RetrofitTianRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianRecipeApi
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.ui.search.CombinedExternalDishImageSource
import com.myorderapp.ui.search.ExternalDishImageSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val TIAN_BASE_URL = "https://apis.tianapi.com/"
private const val SPOONACULAR_BASE_URL = "https://api.spoonacular.com/"
private const val THE_MEAL_DB_BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

val networkModule = module {
    single {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Session Manager (with persistent storage)
    single { SessionManager(androidContext()) }

    single(named("tianapi")) {
        Retrofit.Builder()
            .baseUrl(TIAN_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single(named("spoonacular")) {
        Retrofit.Builder()
            .baseUrl(SPOONACULAR_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single(named("themealdb")) {
        Retrofit.Builder()
            .baseUrl(THE_MEAL_DB_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>(named("tianapi")).create(TianRecipeApi::class.java) }
    single { get<Retrofit>(named("spoonacular")).create(SpoonacularApi::class.java) }
    single { get<Retrofit>(named("themealdb")).create(TheMealDbApi::class.java) }
    single<TianRecipeRemoteDataSource> {
        RetrofitTianRecipeRemoteDataSource(
            api = get(),
            apiKey = ApiConfig.TIAN_API_KEY
        )
    }
    single { SpoonacularExternalDishImageSource(get(), ApiConfig.SPOONACULAR_API_KEY) }
    single { TheMealDbExternalDishImageSource(get()) }
    single<ExternalDishImageSource> {
        CombinedExternalDishImageSource(
            primarySource = get<SpoonacularExternalDishImageSource>(),
            fallbackSource = get<TheMealDbExternalDishImageSource>()
        )
    }
}
