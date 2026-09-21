package com.myorderapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    errorContainer = ErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    background = DarkBackground,
    onBackground = Color(0xFFF6F0EA),
    surface = DarkSurface,
    onSurface = Color(0xFFF6F0EA),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    errorContainer = ErrorContainer
)

@Composable
fun OrderDiskTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        // 系统栏（状态栏/导航栏）与主题背景同源：改背景色自动跟随，永不割裂
        val bgColor = MaterialTheme.colorScheme.background
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            // 图标颜色随背景亮度自动切换（深底→浅图标，浅底→深图标）
            val lightIcons = bgColor.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
