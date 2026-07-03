package com.myorderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

private val LiquidGlassTop = Color(0xF7FFFFFC)
private val LiquidGlassBottom = Color(0xDDF7EFE2)
private val LiquidGlassBorder = Color(0xB8FFFFFF)
private val LiquidGlassHairline = Color(0x55B58B71)
private val LiquidNavSelected = Color(0xFF116F67)
private val LiquidNavMuted = Color(0xFF8B7164)
private val LiquidNavGlow = Color(0xFFFBE7C6)

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .height(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(18.dp, RoundedCornerShape(34.dp), clip = false)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(LiquidGlassTop, LiquidGlassBottom)
                    )
                )
                .border(1.dp, LiquidGlassBorder, RoundedCornerShape(34.dp))
                .border(0.6.dp, LiquidGlassHairline, RoundedCornerShape(34.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .width(118.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.74f))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BottomNavItem.items.forEach { item ->
                    val selected = currentRoute == item.route
                    LiquidBottomNavItem(
                        icon = if (selected) item.selectedIcon else item.unselectedIcon,
                        label = item.title,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabClick(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidBottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val itemShape = RoundedCornerShape(27.dp)
    val color = if (selected) LiquidNavSelected else LiquidNavMuted
    val itemBackground = if (selected) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.88f),
                LiquidNavGlow.copy(alpha = 0.74f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(itemShape)
            .clickable(onClick = onClick)
            .background(itemBackground)
            .then(
                if (selected) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.82f), itemShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .width(26.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.84f))
            )
        }
        Column(
            modifier = Modifier.padding(top = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(if (selected) 23.dp else 22.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
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
