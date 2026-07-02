package com.myorderapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "首页",
        selectedIcon = Icons.Outlined.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Ordering : BottomNavItem(
        route = "ordering",
        title = "点菜",
        selectedIcon = Icons.Outlined.RamenDining,
        unselectedIcon = Icons.Outlined.RamenDining
    )

    data object Discover : BottomNavItem(
        route = "discover",
        title = "发现",
        selectedIcon = Icons.Outlined.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Orders : BottomNavItem(
        route = "orders",
        title = "订单",
        selectedIcon = Icons.AutoMirrored.Outlined.List,
        unselectedIcon = Icons.AutoMirrored.Outlined.List
    )

    data object Profile : BottomNavItem(
        route = "profile",
        title = "我的",
        selectedIcon = Icons.Outlined.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val items = listOf(Home, Ordering, Discover, Orders, Profile)
    }
}
