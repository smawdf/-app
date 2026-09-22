package com.myorderapp.ui.theme

import android.os.Build
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 高糖小食 · 纯净凸透镜水滴液态玻璃（Liquid Drop Glass）
 *
 * 核心光学体系：
 * 1. 【凸透镜折射体感】：内部微凸面纵向弧光（Sheen Arch），中间微凹透光，形成饱满厚度感；
 * 2. 【双向光学刀刃】：顶部 1.5px 纯白高光刃（0.92f）切角反射，底部 1.0px 环境漫反射微晕；
 * 3. 【晶莹微色散边缘】：在圆角边缘形成极其微妙的暖粉/淡冰蓝棱镜微过渡；
 * 4. 【多层全版本兼容】：无需依赖 Android 13 AGSL，无论 Android 9、12 还是 15 都能完美展现极高水滴拟物质感。
 */

// 纯水水滴透光底色
val LiquidDropTintLight = Color(0xFFFFF9F5)
val LiquidDropTintDark = Color(0xFF241A1B)

// 顶部高光刃与月牙反射
val SpecularEdgeLight = Color.White.copy(alpha = 0.98f)
val SpecularEdgeDark = Color.White.copy(alpha = 0.70f)

// 底部环境微反光
val AmbientEdgeLight = Color(0xFFFFD1DC).copy(alpha = 0.60f)
val AmbientEdgeDark = Color.White.copy(alpha = 0.20f)

/**
 * 物理级液态水滴玻璃修饰符（兼容全机型）
 */
fun Modifier.liquidDropGlass(
    shape: Shape = RoundedCornerShape(36.dp),
    isDark: Boolean = false,
    alphaBase: Float = 0.40f
): Modifier = this
    .clip(shape)
    // 1. 饱满透亮底色（兼顾透明穿透与质感显色，绝不死白也不隐形）
    .background(
        Brush.verticalGradient(
            colors = if (!isDark) {
                listOf(
                    Color.White.copy(alpha = 0.68f),
                    Color(0xFFFFF3ED).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.55f)
                )
            } else {
                listOf(
                    Color(0xFF2E2223).copy(alpha = 0.60f),
                    Color(0xFF1F1718).copy(alpha = 0.45f),
                    Color(0xFF2A1E20).copy(alpha = 0.55f)
                )
            }
        )
    )
    // 2. 双向立体光学高光与透镜月牙弧
    .drawWithContent {
        drawContent()

        val specular = if (!isDark) SpecularEdgeLight else SpecularEdgeDark
        val ambient = if (!isDark) AmbientEdgeLight else AmbientEdgeDark

        // ① 顶部 2.0px 极细纯白钻石光刃（Rim Specular）
        drawLine(
            color = specular,
            start = Offset(size.width * 0.10f, 1.5f),
            end = Offset(size.width * 0.90f, 1.5f),
            strokeWidth = 2.4f
        )

        // ② 底部环境微反光线
        drawLine(
            color = ambient,
            start = Offset(size.width * 0.18f, size.height - 1.5f),
            end = Offset(size.width * 0.82f, size.height - 1.5f),
            strokeWidth = 2.0f
        )

        // ③ 凸透镜水滴表面月牙反光弧（核心物理立体感：模拟凸面透镜对顶光的汇聚）
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    specular.copy(alpha = if (!isDark) 0.55f else 0.30f),
                    specular.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                startY = 1f,
                endY = size.height * 0.50f
            ),
            topLeft = Offset(size.width * 0.05f, 1.5f),
            size = Size(size.width * 0.90f, size.height * 0.48f),
            cornerRadius = CornerRadius(size.height * 0.45f, size.height * 0.45f)
        )

        // ④ 边缘棱镜全包围高光轮廓线（上亮下柔，让整个胶囊水滴轮廓晶莹浮现）
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    specular.copy(alpha = 0.95f),
                    Color(0xFFFF9EB5).copy(alpha = 0.50f),
                    ambient.copy(alpha = 0.70f)
                ),
                startY = 0f,
                endY = size.height
            ),
            size = size,
            cornerRadius = CornerRadius(size.height * 0.5f, size.height * 0.5f),
            style = Stroke(width = 2.0f)
        )
    }

/**
 * 历史兼容版毛玻璃卡片
 */
val GlassTintLight = Color(0xCCFFF8F0)
val GlassBorderLight = Color(0xFFFFFFFFFF).copy(alpha = 0.72f)
val GlassTintDark = Color(0x992B1F1C)
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.16f)

@Composable
fun glassTint(): Color =
    if (isSystemInDarkTheme()) GlassTintDark else GlassTintLight

@Composable
fun glassBorder(): Color =
    if (isSystemInDarkTheme()) GlassBorderDark else GlassBorderLight

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isDark) Color(0x992B1F1C) else Color(0xCCFFF8F0))
            .border(borderWidth, if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.72f), shape)
    ) {
        content()
    }
}
