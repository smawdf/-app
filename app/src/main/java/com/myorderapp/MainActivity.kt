package com.myorderapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.sync.CloudSyncCoordinator
import com.myorderapp.ui.components.CozyMainTopBar
import com.myorderapp.ui.navigation.BottomNavItem
import com.myorderapp.ui.navigation.NavGraph
import com.myorderapp.ui.navigation.Routes
import com.myorderapp.ui.navigation.navigateAsTab
import com.myorderapp.ui.theme.Background
import com.myorderapp.ui.theme.OrderDiskTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.koin.compose.getKoin

class MainActivity : ComponentActivity() {
    private var latestDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        latestDeepLink = intent?.data?.toString()
        setContent {
            OrderDiskTheme {
                MainScreen(initialDeepLink = latestDeepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestDeepLink = intent.data?.toString()
    }
}

@Composable
fun MainScreen(initialDeepLink: String? = null) {
    val sessionManager = getKoin().get<SessionManager>()
    val cloudSyncCoordinator = getKoin().get<CloudSyncCoordinator>()
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle()
    val restoredSessionAtStartup = remember { isLoggedIn }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val tabRoutes = BottomNavItem.items.map { it.route }.toSet()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val startDestination = remember(initialDeepLink) {
        if (initialDeepLink?.startsWith("orderdisk://auth/reset-password") == true) {
            Routes.resetPassword(initialDeepLink)
        } else if (initialDeepLink?.startsWith("orderdisk://auth/switch-device") == true) {
            Routes.deviceSwitch(initialDeepLink)
        } else if (isLoggedIn) {
            Routes.HOME
        } else {
            Routes.AUTH
        }
    }
    var handledDeepLink by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(restoredSessionAtStartup) {
        if (restoredSessionAtStartup) cloudSyncCoordinator.syncInBackground()
    }
    LaunchedEffect(initialDeepLink, currentRoute) {
        val link = initialDeepLink ?: return@LaunchedEffect
        if (handledDeepLink == link || currentRoute == null) return@LaunchedEffect
        val destination = when {
            link.startsWith("orderdisk://auth/reset-password") ->
                Routes.RESET_PASSWORD to Routes.resetPassword(link)
            link.startsWith("orderdisk://auth/switch-device") ->
                Routes.DEVICE_SWITCH to Routes.deviceSwitch(link)
            else -> null
        }
        if (destination != null && currentRoute != destination.first) {
            navController.navigate(destination.second) {
                launchSingleTop = true
            }
        }
        handledDeepLink = link
    }

    val shellRoute = currentRoute ?: startDestination.takeIf { it in tabRoutes }
    val showMainShell = shellRoute in tabRoutes
    LaunchedEffect(showMainShell) {
        if (
            showMainShell &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val mainTopBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp


    Box(modifier = Modifier.fillMaxSize()) {
        NavGraph(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (showMainShell) mainTopBarHeight else 0.dp),
            startDestination = startDestination,
            resetPasswordDeepLink = initialDeepLink.orEmpty()
        )
        if (showMainShell) {
            CozyMainTopBar(
                title = shellRoute.mainTabTopBarTitle(),
                containerColor = shellRoute.mainTabTopBarContainerColor(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(20f)
            )
            FloatingLiquidBottomBar(
                currentRoute = currentRoute,
                onTabClick = { route -> navController.navigateAsTab(route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(20f)
            )
        }
    }
}


private fun String?.mainTabTopBarTitle(): String = when (this) {
    Routes.HOME -> "今天也要一起好好吃饭"
    Routes.ORDERING -> "点菜 - 挑选今日美味"
    Routes.DISCOVER -> "发现"
    Routes.ORDERS -> "订单 - 甜蜜点菜记录"
    Routes.PROFILE -> "个人中心"
    else -> ""
}

private fun String?.mainTabTopBarContainerColor(): Color = Background

@Composable
private fun FloatingLiquidBottomBar(
    currentRoute: String?,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = bottomPadding + 14.dp)
            .fillMaxWidth()
            .widthIn(max = 430.dp)
            .height(68.dp),
        contentAlignment = Alignment.Center
    ) {
        val selectedIndex = BottomNavItem.items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
        BoxWithConstraints(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xE6FFFFFF),
                            Color(0xAFFFF8FB),
                            Color(0x91FFF1F6)
                        )
                    )
                )
                .border(1.dp, Color(0xC9FFFFFF), RoundedCornerShape(36.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            val tabWidth = maxWidth / BottomNavItem.items.size
            val heartOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex + (tabWidth - 58.dp) / 2,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "heartNavOffset"
            )
            LiquidGlassNavLayer(modifier = Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .offset { IntOffset(heartOffset.roundToPx(), 0) }
                    .size(width = 58.dp, height = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                HeartNavIndicator()
            }
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem.items.forEach { item ->
                    val selected = currentRoute == item.route
                    val tint = if (selected) Color(0xFF894C5C) else Color(0xFF8C8480)
                    val interaction = remember(item.route) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(if (pressed) 0.96f else 1f)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) { onTabClick(item.route) },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = tint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Text(
                            text = item.title,
                            color = tint,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassNavLayer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = Color.White.copy(alpha = 0.48f),
            radius = size.height * 0.62f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.18f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.34f),
            radius = size.height * 0.46f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.22f)
        )
    }
}

@Composable
private fun HeartNavIndicator() {
    Canvas(modifier = Modifier.size(58.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.84f)
            cubicTo(w * 0.44f, h * 0.78f, w * 0.31f, h * 0.67f, w * 0.20f, h * 0.56f)
            cubicTo(w * 0.05f, h * 0.42f, w * 0.08f, h * 0.18f, w * 0.32f, h * 0.18f)
            cubicTo(w * 0.42f, h * 0.18f, w * 0.48f, h * 0.25f, w * 0.50f, h * 0.33f)
            cubicTo(w * 0.52f, h * 0.25f, w * 0.58f, h * 0.18f, w * 0.68f, h * 0.18f)
            cubicTo(w * 0.92f, h * 0.18f, w * 0.95f, h * 0.42f, w * 0.80f, h * 0.56f)
            cubicTo(w * 0.69f, h * 0.67f, w * 0.56f, h * 0.78f, w * 0.50f, h * 0.84f)
            close()
        }
        drawPath(path, Color(0xFFFFD1DC).copy(alpha = 0.58f))
        drawPath(path, Color.White.copy(alpha = 0.20f))
    }
}
