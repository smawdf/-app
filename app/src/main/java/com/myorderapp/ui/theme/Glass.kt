package com.myorderapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 高糖小食 · 纯清水滴透明液态玻璃（Ultra-Clear Liquid Glass）规范。
 * 
 * 核心光学原则：
 * 1. 绝不死白、绝不磨砂起雾：背景透明度保持在 4%~8% 的超清澈水光状态，背后菜品与文字 100% 清晰看穿；
 * 2. 靠光而非靠底色定义形态：依靠顶部 1.5px 极细纯白高光刃（Rim Specular）与底部环境微反光勾勒水滴轮廓；
 * 3. 内部月牙水光（Water Sheen）：模拟光线斜射在水珠凸面上方产生的灵动镜面反光。
 */

// 纯水级超高透底色（浅色模式 5% 清水白，深色模式 12% 晶透暗水）
val UltraClearWaterTintLight = Color.White.copy(alpha = 0.05f)
val UltraClearWaterTintDark = Color(0xFF1E1618).copy(alpha = 0.15f)

// 纯白高光反光刃
val WaterRimSpecularLight = Color.White.copy(alpha = 0.95f)
val WaterRimSpecularDark = Color.White.copy(alpha = 0.65f)

// 水滴边缘轮廓线
val WaterBorderLight = Color.White.copy(alpha = 0.70f)
val WaterBorderDark = Color.White.copy(alpha = 0.25f)

// 兼容历史旧值
val GlassTintLight = Color(0xCCFFF8F0)
val GlassBorderLight = Color(0xFFFFFFFFFF).copy(alpha = 0.72f)
val GlassTintDark = Color(0x992B1F1C)
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.16f)

@Composable
fun glassTint(): Color =
    if (isDarkTheme()) GlassTintDark else GlassTintLight

@Composable
fun glassBorder(): Color =
    if (isDarkTheme()) GlassBorderDark else GlassBorderLight

@Composable
fun isDarkTheme(): Boolean =
    isSystemInDarkTheme()

/**
 * 历史兼容版毛玻璃卡片（保留供旧业务平稳过渡）。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(glassTint())
            .border(borderWidth, glassBorder(), shape)
    ) {
        content()
    }
}

/**
 * 真·纯清水滴超透液态玻璃卡片（Ultra-Clear Liquid Glass Card）。
 * 彻底剔除磨砂白雾，背后的内容完全清晰看穿，带有饱满的水滴高光反光刃。
 */
@Composable
fun UltraClearLiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    borderWidth: Dp = 1.2.dp,
    showWaterSheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val waterTint = if (isDark) UltraClearWaterTintDark else UltraClearWaterTintLight
    val borderColor = if (isDark) WaterBorderDark else WaterBorderLight
    val specularColor = if (isDark) WaterRimSpecularDark else WaterRimSpecularLight

    Box(
        modifier = modifier
            .clip(shape)
            // 1. 极其透彻的清水基底（仅 5% 不透明度）
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        waterTint.copy(alpha = if (isDark) 0.18f else 0.08f),
                        waterTint.copy(alpha = if (isDark) 0.08f else 0.02f)
                    )
                )
            )
            // 2. 水光刃精细描边
            .border(borderWidth, borderColor, shape)
            // 3. 顶部月牙形水光反光（Water Sheen）
            .then(
                if (showWaterSheen) {
                    Modifier.drawWithContent {
                        drawContent()
                        // 绘制顶部高光切角反光线
                        drawLine(
                            color = specularColor,
                            start = Offset(size.width * 0.12f, 1.2f),
                            end = Offset(size.width * 0.88f, 1.2f),
                            strokeWidth = 2.0f
                        )
                        // 绘制水滴顶部凸透镜弧光反光条
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    specularColor.copy(alpha = if (isDark) 0.18f else 0.32f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.45f
                            ),
                            topLeft = Offset(size.width * 0.08f, 0f),
                            size = Size(size.width * 0.84f, size.height * 0.45f)
                        )
                    }
                } else Modifier
            )
    ) {
        content()
    }
}
