package com.myorderapp.ui.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myorderapp.ui.auth.AuthScreen
import com.myorderapp.ui.auth.ResetPasswordScreen
import com.myorderapp.ui.cart.CartScreen
import com.myorderapp.ui.candy.CandyCoinsScreen
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
    const val CANDY_COINS = "candy_coins"
    const val AUTH = "auth"
    const val RESET_PASSWORD = "reset_password?deepLink={deepLink}"
    const val ONBOARDING = "onboarding"
    const val ANNIVERSARY = "anniversary"

    fun orderDetail(orderId: String) = "orders/$orderId"
    fun resetPassword(deepLink: String) = "reset_password?deepLink=${Uri.encode(deepLink)}"
}

private const val ANIM_DURATION = 220
private const val EXIT_ANIM_DURATION = 140

private fun enterSlideFade(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { it / 10 }
    ) + fadeIn(tween(ANIM_DURATION, easing = FastOutSlowInEasing))

private fun exitSlideFade(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(EXIT_ANIM_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { -it / 18 }
    ) + fadeOut(tween(EXIT_ANIM_DURATION, easing = FastOutSlowInEasing))

private fun popEnterSlideFade(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { -it / 10 }
    ) + fadeIn(tween(ANIM_DURATION, easing = FastOutSlowInEasing))

private fun popExitSlideFade(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(EXIT_ANIM_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { it / 18 }
    ) + fadeOut(tween(EXIT_ANIM_DURATION, easing = FastOutSlowInEasing))

fun NavHostController.navigateAsTab(route: String) {
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
    startDestination: String = Routes.HOME,
    resetPasswordDeepLink: String = ""
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
        composable(Routes.HOME) {
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

        composable(Routes.DISCOVER) {
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

        composable(Routes.ORDERS) {
            OrdersScreen(
                onOrderClick = { orderId -> navController.navigate(Routes.orderDetail(orderId)) },
                onGoOrderingClick = { navController.navigateAsTab(Routes.ORDERING) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onLoginClick = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onDishManageClick = { navController.navigate(Routes.SHOP_SETTINGS) },
                onOrdersClick = { navController.navigateAsTab(Routes.ORDERS) },
                onCandyCoinsClick = { navController.navigate(Routes.CANDY_COINS) }
            )
        }

        composable(Routes.CANDY_COINS) {
            CandyCoinsScreen(onBack = { navController.popBackStack() })
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onForgotPasswordClick = {
                    navController.navigate(Routes.resetPassword(""))
                }
            )
        }

        composable(
            route = Routes.RESET_PASSWORD,
            arguments = listOf(navArgument("deepLink") {
                type = NavType.StringType
                defaultValue = resetPasswordDeepLink
            })
        ) { backStackEntry ->
            val deepLink = backStackEntry.arguments?.getString("deepLink").orEmpty()
            ResetPasswordScreen(
                deepLink = deepLink,
                onBackToLogin = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
