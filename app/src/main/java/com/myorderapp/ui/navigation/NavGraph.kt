package com.myorderapp.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myorderapp.ui.auth.AuthScreen
import com.myorderapp.ui.cart.CartScreen
import com.myorderapp.ui.checkout.CheckoutScreen
import com.myorderapp.ui.couple.AnniversaryScreen
import com.myorderapp.ui.couple.CoupleMenuScreen
import com.myorderapp.ui.discover.DiscoverScreen
import com.myorderapp.ui.menu.MenuManagementScreen
import com.myorderapp.ui.onboarding.OnboardingScreen
import com.myorderapp.ui.order.OrderingScreen
import com.myorderapp.ui.orders.OrderDetailScreen
import com.myorderapp.ui.orders.OrdersScreen
import com.myorderapp.ui.profile.ProfileScreen

object Routes {
    const val HOME = "home"
    const val ORDERING = "ordering"
    const val MENU_MANAGEMENT = "menu_management"
    const val SHOP_SETTINGS = "shop_settings"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val DISCOVER = "discover"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "orders/{orderId}"
    const val PROFILE = "profile"
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val ANNIVERSARY = "anniversary"

    fun orderDetail(orderId: String) = "orders/$orderId"
}

private const val ANIM_DURATION = 260

private fun enterSlideFade(): EnterTransition =
    fadeIn(tween(ANIM_DURATION))

private fun exitSlideFade(): ExitTransition =
    fadeOut(tween(ANIM_DURATION))

private fun popEnterSlideFade(): EnterTransition =
    fadeIn(tween(ANIM_DURATION))

private fun popExitSlideFade(): ExitTransition =
    fadeOut(tween(ANIM_DURATION))

private fun NavHostController.navigateAsTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { enterSlideFade() },
        exitTransition = { exitSlideFade() },
        popEnterTransition = { popEnterSlideFade() },
        popExitTransition = { popExitSlideFade() }
    ) {
        composable(
            Routes.HOME,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            CoupleMenuScreen(
                onCustomizeMenuClick = { navController.navigate(Routes.SHOP_SETTINGS) },
                onGoOrderingClick = { navController.navigateAsTab(Routes.ORDERING) },
                onAnniversaryClick = { navController.navigate(Routes.ANNIVERSARY) },
                onOrdersClick = { navController.navigateAsTab(Routes.ORDERS) },
                onProfileClick = { navController.navigateAsTab(Routes.PROFILE) }
            )
        }

        composable(Routes.ANNIVERSARY) {
            AnniversaryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ORDERING) {
            OrderingScreen(
                onShopNameClick = { navController.navigate(Routes.SHOP_SETTINGS) },
                onManageMenuClick = { navController.navigate(Routes.SHOP_SETTINGS) },
                onCheckoutClick = { navController.navigate(Routes.CHECKOUT) }
            )
        }

        composable(
            Routes.DISCOVER,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            DiscoverScreen()
        }

        composable(Routes.MENU_MANAGEMENT) {
            MenuManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SHOP_SETTINGS) {
            MenuManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CART) {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutClick = { navController.navigate(Routes.CHECKOUT) }
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onOrderSubmitted = { orderId ->
                    navController.navigate(Routes.orderDetail(orderId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ORDERS,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            OrdersScreen(
                onOrderClick = { orderId -> navController.navigate(Routes.orderDetail(orderId)) },
                onGoOrderingClick = { navController.navigateAsTab(Routes.ORDERING) }
            )
        }

        composable(
            Routes.PROFILE,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            ProfileScreen(
                onLoginClick = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onDishManageClick = { navController.navigate(Routes.SHOP_SETTINGS) }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onRegisterComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                onLoggedIn = {
                    navController.popBackStack()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
    }
}
