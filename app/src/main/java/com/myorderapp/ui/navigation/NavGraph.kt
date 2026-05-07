package com.myorderapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myorderapp.ui.adddish.AddDishScreen
import com.myorderapp.ui.auth.AuthScreen
import com.myorderapp.ui.dishdetail.DishDetailScreen
import com.myorderapp.ui.dishlibrary.DishLibraryScreen
import com.myorderapp.ui.history.HistoryScreen
import com.myorderapp.ui.home.HomeScreen
import com.myorderapp.ui.meal.MealResultScreen
import com.myorderapp.ui.meal.StartMealScreen
import com.myorderapp.ui.onboarding.OnboardingScreen
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.ui.profile.ProfileScreen
import com.myorderapp.ui.random.RandomScreen
import com.myorderapp.ui.search.SearchScreen
import com.myorderapp.ui.wishlist.WishlistScreen

object Routes {
    const val HOME = "home"
    const val DISH_LIBRARY = "dish_library"
    const val MEAL = "meal"
    const val WISHLIST = "wishlist"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val DISH_DETAIL = "dish_detail/{dishId}/{source}"
    const val ADD_DISH = "add_dish"
    const val START_MEAL = "start_meal/{mealType}"
    const val MEAL_RESULT = "meal_result/{mealId}"
    const val RANDOM = "random"
    const val HISTORY = "history"
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val PROFILE_SETUP = "profile_setup"

    fun dishDetail(dishId: String, source: String) = "dish_detail/$dishId/$source"
    fun startMeal(mealType: String) = "start_meal/$mealType"
    fun mealResult(mealId: String) = "meal_result/$mealId"
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
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onRandomClick = { navController.navigate(Routes.RANDOM) },
                onAddDishClick = { navController.navigate(Routes.ADD_DISH) },
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                },
                onStartMeal = { navController.navigate(Routes.startMeal("lunch")) },
                onHistoryClick = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.DISH_LIBRARY) {
            DishLibraryScreen(
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                },
                onAddDishClick = { navController.navigate(Routes.ADD_DISH) }
            )
        }
        composable(Routes.MEAL) {
            StartMealScreen(
                onResultClick = { mealId ->
                    navController.navigate(Routes.mealResult(mealId))
                }
            )
        }
        composable(Routes.WISHLIST) {
            WishlistScreen(
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                }
            )
        }
        composable(Routes.PROFILE) {
            val sessionManager = org.koin.compose.getKoin().get<SessionManager>()
            ProfileScreen(
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
                onLoginClick = { navController.navigate(Routes.AUTH) },
                onDishManageClick = {
                    navController.navigate(Routes.DISH_LIBRARY) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                onLogoutClick = {
                    sessionManager.clear()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                }
            )
        }
        composable(
            route = Routes.DISH_DETAIL,
            arguments = listOf(
                navArgument("dishId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dishId = backStackEntry.arguments?.getString("dishId") ?: ""
            val source = backStackEntry.arguments?.getString("source") ?: "custom"
            DishDetailScreen(
                dishId = dishId,
                source = source,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADD_DISH) {
            AddDishScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.START_MEAL,
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealType = backStackEntry.arguments?.getString("mealType") ?: "lunch"
            StartMealScreen(
                initialMealType = mealType,
                onResultClick = { mealId ->
                    navController.navigate(Routes.mealResult(mealId))
                }
            )
        }
        composable(
            route = Routes.MEAL_RESULT,
            arguments = listOf(navArgument("mealId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId") ?: ""
            MealResultScreen(
                mealId = mealId,
                onBack = { navController.popBackStack() },
                onNewMeal = {
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
        composable(Routes.RANDOM) {
            RandomScreen(
                onBack = { navController.popBackStack() },
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                }
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
                }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
