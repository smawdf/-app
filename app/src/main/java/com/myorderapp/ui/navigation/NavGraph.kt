package com.myorderapp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.ui.about.AboutScreen
import com.myorderapp.ui.adddish.AddDishScreen
import com.myorderapp.ui.auth.AuthScreen
import com.myorderapp.ui.dishdetail.DishDetailScreen
import com.myorderapp.ui.dishlibrary.DishLibraryScreen
import com.myorderapp.ui.history.HistoryScreen
import com.myorderapp.ui.home.HomeScreen
import com.myorderapp.ui.meal.MealResultScreen
import com.myorderapp.ui.meal.StartMealScreen
import com.myorderapp.ui.onboarding.OnboardingScreen
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
    const val ADD_DISH = "add_dish/{editDishId}"
    fun addDish(editDishId: String? = null) = "add_dish/${editDishId ?: "new"}"
    const val START_MEAL = "start_meal/{mealType}"
    const val MEAL_RESULT = "meal_result/{mealId}"
    const val RANDOM = "random"
    const val HISTORY = "history"
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val PROFILE_SETUP = "profile_setup"
    const val ABOUT = "about"

    fun dishDetail(dishId: String, source: String) = "dish_detail/$dishId/$source"
    fun startMeal(mealType: String) = "start_meal/$mealType"
    fun mealResult(mealId: String) = "meal_result/$mealId"
}

// Shared animation specs
private const val ANIM_DURATION = 300
private val enterSlide = slideInHorizontally(tween(ANIM_DURATION)) { it }
private val exitSlide = slideOutHorizontally(tween(ANIM_DURATION)) { -it }
private val popEnterSlide = slideInHorizontally(tween(ANIM_DURATION)) { -it }
private val popExitSlide = slideOutHorizontally(tween(ANIM_DURATION)) { it }
private val enterFade = fadeIn(tween(ANIM_DURATION))
private val exitFade = fadeOut(tween(ANIM_DURATION))
private val enterSlideFade = enterSlide + enterFade
private val exitSlideFade = exitSlide + exitFade
private val popEnterSlideFade = popEnterSlide + enterFade
private val popExitSlideFade = popExitSlide + exitFade

// Scale-down for push transitions (subtle depth effect)
private val enterScale = scaleIn(tween(ANIM_DURATION), initialScale = 0.92f)
private val exitScale = scaleOut(tween(ANIM_DURATION), targetScale = 0.92f)

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
        enterTransition = { enterSlideFade },
        exitTransition = { exitSlideFade + exitScale },
        popEnterTransition = { popEnterSlideFade },
        popExitTransition = { popExitSlideFade + exitScale }
    ) {
        // ── Bottom nav tabs: crossfade instead of slide ──
        composable(
            Routes.HOME,
            enterTransition = { enterFade },
            exitTransition = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition = { exitFade }
        ) {
            HomeScreen(
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onRandomClick = { navController.navigate(Routes.RANDOM) },
                onAddDishClick = { navController.navigate(Routes.addDish()) },
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                },
                onStartMeal = { navController.navigate(Routes.startMeal("lunch")) },
                onHistoryClick = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(
            Routes.DISH_LIBRARY,
            enterTransition = { enterFade },
            exitTransition = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition = { exitFade }
        ) {
            DishLibraryScreen(
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                },
                onAddDishClick = { navController.navigate(Routes.addDish()) }
            )
        }
        composable(
            Routes.MEAL,
            enterTransition = { enterFade },
            exitTransition = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition = { exitFade }
        ) {
            StartMealScreen(
                onResultClick = { mealId ->
                    navController.navigate(Routes.mealResult(mealId))
                }
            )
        }
        composable(
            Routes.WISHLIST,
            enterTransition = { enterFade },
            exitTransition = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition = { exitFade }
        ) {
            WishlistScreen(
                onDishClick = { dishId, source ->
                    navController.navigate(Routes.dishDetail(dishId, source))
                }
            )
        }
        composable(
            Routes.PROFILE,
            enterTransition = { enterFade },
            exitTransition = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition = { exitFade }
        ) {
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
                onAboutClick = { navController.navigate(Routes.ABOUT) },
                onLogoutClick = {
                    sessionManager.clear()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Push screens: slide from right ──
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
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(Routes.addDish(id)) }
            )
        }
        composable(
            route = Routes.ADD_DISH,
            arguments = listOf(navArgument("editDishId") {
                type = NavType.StringType; defaultValue = "new"
            })
        ) { backStackEntry ->
            val editDishId = backStackEntry.arguments?.getString("editDishId")
                ?.takeIf { it != "new" }
            AddDishScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                editDishId = editDishId
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
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
