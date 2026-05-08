package com.myorderapp.di

import com.myorderapp.ApiConfig
import com.myorderapp.data.local.RecipeAssetLoader
import com.myorderapp.data.remote.recipe.JisuRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.InMemoryDishRepository
import com.myorderapp.data.repository.InMemoryMealRepository
import com.myorderapp.data.repository.InMemoryProfileRepository
import com.myorderapp.data.repository.InMemoryWishlistRepository
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
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Data sources
    single { com.myorderapp.data.remote.supabase.SupabaseStorageUploader(get(), ApiConfig.SUPABASE_URL) }
    single { JuheRecipeRemoteDataSource(get(), ApiConfig.JUHE_API_KEY) }
    single { TianRecipeRemoteDataSource(get(), ApiConfig.TIAN_API_KEY) }
    single { JisuRecipeRemoteDataSource(get(), ApiConfig.JISU_API_KEY) }
    // Use cases
    single { DualRecipeSearchUseCase(get(), get(), get(), get<DishRepository>()) }

    // Repositories — online when logged in, local as fallback
    // Dish
    single { RecipeAssetLoader(androidContext()) }
    single { InMemoryDishRepository(androidContext(), get<RecipeAssetLoader>().loadRecipes()) }
    single { SupabaseDishRepository(get(), get(), androidContext().filesDir) }
    single { HybridDishRepository(get(), get(), get()) }
    single<DishRepository> { get<HybridDishRepository>() }
    // Profile
    single { InMemoryProfileRepository() }
    single { SupabaseProfileRepository(get(), get(), androidContext()) }
    single<ProfileRepository> { get<SupabaseProfileRepository>() }
    // Meal
    single { InMemoryMealRepository() }
    single { SupabaseMealRepository(get(), get(), get()) }
    single<MealRepository> { get<SupabaseMealRepository>() }
    single<WishlistRepository> { InMemoryWishlistRepository() }

    // ViewModels
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { DishDetailViewModel(get(), get()) }
    viewModel { DishLibraryViewModel(get()) }
    viewModel { AddDishViewModel(get(), get(), get(), androidContext()) }
    viewModel { MealViewModel(get(), get(), get(), get()) }
    viewModel { RandomViewModel(get(), get(), androidContext()) }
    viewModel { WishlistViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
