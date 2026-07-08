package com.myorderapp.di

import com.myorderapp.data.local.AppDatabase
import com.myorderapp.data.local.BimissingRecipeAssetSource
import com.myorderapp.data.local.RecipeAssetLoader
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.RoomDishRepository
import com.myorderapp.data.repository.InMemoryProfileRepository
import com.myorderapp.data.repository.RoomAddressRepository
import com.myorderapp.data.repository.RoomCartRepository
import com.myorderapp.data.repository.RoomCandyCoinLedgerRepository
import com.myorderapp.data.repository.RoomMenuRepository
import com.myorderapp.data.repository.SingleShopRepository
import com.myorderapp.data.repository.SupabaseDishRepository
import com.myorderapp.data.repository.SupabaseOrderRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.domain.repository.AddressRepository
import com.myorderapp.domain.repository.CartRepository
import com.myorderapp.domain.repository.CandyCoinLedgerRepository
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.domain.repository.ShopRepository
import com.myorderapp.ui.address.AddressViewModel
import com.myorderapp.ui.auth.AuthViewModel
import com.myorderapp.ui.cart.CartViewModel
import com.myorderapp.ui.checkout.CheckoutViewModel
import com.myorderapp.ui.candy.CandyCoinsViewModel
import com.myorderapp.ui.discover.DiscoverViewModel
import com.myorderapp.ui.menu.MenuManagementViewModel
import com.myorderapp.ui.onboarding.OnboardingViewModel
import com.myorderapp.ui.order.OrderingViewModel
import com.myorderapp.ui.orders.OrderDetailViewModel
import com.myorderapp.ui.orders.OrdersViewModel
import com.myorderapp.ui.profile.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Data sources
    single { SupabaseStorageUploader(get()) }
    // Database
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().dishDao() }
    single { get<AppDatabase>().menuDishDao() }
    single { get<AppDatabase>().cartDao() }
    single { get<AppDatabase>().addressDao() }
    single { get<AppDatabase>().orderDao() }
    single { get<AppDatabase>().candyCoinRecordDao() }

    // Repositories — online when logged in, local as fallback
    // Dish
    single { RecipeAssetLoader(androidContext()) }
    single { BimissingRecipeAssetSource(androidContext()) }
    single { RoomDishRepository(get()) }
    single { RoomMenuRepository(get()) }
    single<SingleShopRepository> { SingleShopRepository(androidContext(), get()) }
    single { SupabaseDishRepository(get(), androidContext().filesDir) }
    single { HybridDishRepository(get(), get(), get(), get()) }
    single<DishRepository> { get<HybridDishRepository>() }
    // Profile
    single { InMemoryProfileRepository() }
    single<CandyCoinLedgerRepository> { RoomCandyCoinLedgerRepository(get()) }
    single { SupabaseProfileRepository(get(), androidContext(), get()) }
    single<ProfileRepository> { get<SupabaseProfileRepository>() }
    single<ShopRepository> { get<SingleShopRepository>() }
    single<MenuRepository> { get<SingleShopRepository>() }
    single<CartRepository> { RoomCartRepository(get()) }
    single<AddressRepository> { RoomAddressRepository(get()) }
    single<OrderRepository> { SupabaseOrderRepository(get(), get(), get(), get()) }

    // ViewModels
    viewModelOf(::OrderingViewModel)
    viewModelOf(::MenuManagementViewModel)
    viewModelOf(::CartViewModel)
    viewModel { DiscoverViewModel(
            dishRepository = get(),
            menuRepository = get(),
            roomMenuRepository = get(),
            bimissingRecipeAssetSource = get(),
            externalDishImageSource = get(),
            xiachufangRecipeSearchSource = get(),
            tianRecipeRemoteDataSource = get()
        )
    }
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::CandyCoinsViewModel)
    viewModel { AddressViewModel(get(), get()) }
    viewModel { OrdersViewModel(get()) }
    viewModel { OrderDetailViewModel(get()) }
}
