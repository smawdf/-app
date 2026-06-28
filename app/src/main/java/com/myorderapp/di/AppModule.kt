package com.myorderapp.di

import com.myorderapp.data.local.AppDatabase
import com.myorderapp.data.local.RecipeAssetLoader
import com.myorderapp.data.remote.recipe.JisuRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.supabase.RealtimeService
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.RoomDishRepository
import com.myorderapp.data.repository.RoomPagingDishRepository
import com.myorderapp.data.repository.InMemoryMealRepository
import com.myorderapp.data.repository.InMemoryProfileRepository
import com.myorderapp.data.repository.RoomWishlistRepository
import com.myorderapp.data.repository.SupabaseDishRepository
import com.myorderapp.data.repository.SupabaseMealRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.MealRepository
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.domain.repository.WishlistRepository
import com.myorderapp.domain.usecase.DualRecipeSearchUseCase
import com.myorderapp.ui.adddish.AddDishViewModel
import com.myorderapp.ui.auth.AuthViewModel
import com.myorderapp.ui.dishdetail.DishDetailViewModel
import com.myorderapp.ui.dishlibrary.DishLibraryViewModel
import com.myorderapp.ui.history.HistoryViewModel
import com.myorderapp.ui.home.HomeViewModel
import com.myorderapp.ui.meal.MealViewModel
import com.myorderapp.ui.onboarding.OnboardingViewModel
import com.myorderapp.ui.profile.ProfileViewModel
import com.myorderapp.ui.random.RandomViewModel
import com.myorderapp.ui.search.SearchViewModel
import com.myorderapp.ui.wishlist.WishlistViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Data sources
    single { SupabaseStorageUploader(get()) }
    single { RealtimeService() }
    single { JuheRecipeRemoteDataSource(get(), com.myorderapp.ApiConfig.JUHE_API_KEY) }
    single { TianRecipeRemoteDataSource(get(), com.myorderapp.ApiConfig.TIAN_API_KEY) }
    single { JisuRecipeRemoteDataSource(get(), com.myorderapp.ApiConfig.JISU_API_KEY) }
    // Use cases
    single { DualRecipeSearchUseCase(get(), get(), get(), get<DishRepository>()) }

    // Database
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().dishDao() }
    single { get<AppDatabase>().wishlistDao() }

    // Repositories — online when logged in, local as fallback
    // Dish
    single { RecipeAssetLoader(androidContext()) }
    single { RoomDishRepository(get()) }
    single { RoomPagingDishRepository(get()) }
    single { SupabaseDishRepository(get(), androidContext().filesDir) }
    single { HybridDishRepository(get(), get(), get()) }
    single<DishRepository> { get<HybridDishRepository>() }
    // Profile
    single { InMemoryProfileRepository() }
    single { SupabaseProfileRepository(get(), androidContext()) }
    single<ProfileRepository> { get<SupabaseProfileRepository>() }
    // Meal
    single { InMemoryMealRepository() }
    single { SupabaseMealRepository(get(), get()) }
    single<MealRepository> { get<SupabaseMealRepository>() }
    single<WishlistRepository> { RoomWishlistRepository(get()) }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::DishDetailViewModel)
    viewModel { DishLibraryViewModel(get<HybridDishRepository>(), get()) }
    viewModel { AddDishViewModel(get(), get(), androidContext()) }
    viewModelOf(::MealViewModel)
    viewModel { RandomViewModel(get(), get(), androidContext()) }
    viewModelOf(::WishlistViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::OnboardingViewModel)
}
