package com.myorderapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = Routes.HOME,
        title = "首页",
        selectedIcon = Icons.Outlined.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Ordering : BottomNavItem(
        route = Routes.ORDERING,
        title = "点餐",
        selectedIcon = Icons.Outlined.Restaurant,
        unselectedIcon = Icons.Outlined.Restaurant
    )

    data object Discover : BottomNavItem(
        route = Routes.DISCOVER,
        title = "发现",
        selectedIcon = Icons.Outlined.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Orders : BottomNavItem(
        route = Routes.ORDERS,
        title = "订单",
        selectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong
    )

    data object Profile : BottomNavItem(
        route = Routes.PROFILE,
        title = "我的",
        selectedIcon = Icons.Outlined.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val items = listOf(Home, Ordering, Discover, Orders, Profile)
    }
}
