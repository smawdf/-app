package com.myorderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myorderapp.ui.navigation.BottomNavItem
import com.myorderapp.ui.navigation.NavGraph
import com.myorderapp.ui.navigation.Routes
import com.myorderapp.ui.theme.OrderDiskTheme
import com.myorderapp.data.remote.supabase.SessionManager
import org.koin.compose.getKoin

private val WarmNavSurface = Color(0xFFFFFBF5)
private val WarmNavBorder = Color(0xFFF2DAC7)
private val WarmNavSelected = Color(0xFF119482)
private val WarmNavMuted = Color(0xFF8C6650)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrderDiskTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val sessionManager = getKoin().get<SessionManager>()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val startDestination = remember {
        if (sessionManager.isLoggedIn.value) Routes.HOME else Routes.ONBOARDING
    }

    val bottomNavRoutes = BottomNavItem.items.map { it.route }
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                WarmBottomBar(
                    currentRoute = currentDestination?.route,
                    onTabClick = { route ->
                        if (currentDestination?.route != route) {
                            navController.navigateAsTab(route)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = startDestination
        )
    }
}

@Composable
private fun WarmBottomBar(
    currentRoute: String?,
    onTabClick: (String) -> Unit
) {
    Surface(
        color = WarmNavSurface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmNavBorder),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(70.dp)
                .background(WarmNavSurface)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.items.forEach { item ->
                val selected = currentRoute == item.route
                WarmBottomNavItem(
                    icon = item.unselectedIcon,
                    label = item.title,
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabClick(item.route) }
                )
            }
        }
    }
}

@Composable
private fun WarmBottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) WarmNavSelected else WarmNavMuted
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(23.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private fun NavHostController.navigateAsTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
