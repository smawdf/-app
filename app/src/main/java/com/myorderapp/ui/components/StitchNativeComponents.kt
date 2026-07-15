package com.myorderapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.ui.theme.Background
import com.myorderapp.ui.theme.OnSurface
import com.myorderapp.ui.theme.OnSurfaceVariant
import com.myorderapp.ui.theme.OutlineVariant
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.PrimaryContainer
import com.myorderapp.ui.theme.Secondary
import com.myorderapp.ui.theme.SecondaryContainer
import com.myorderapp.ui.theme.Surface
import com.myorderapp.ui.theme.SurfaceContainerLow
import com.myorderapp.ui.theme.Tertiary
import com.myorderapp.ui.theme.TertiaryContainer

val CozyCream = Background
val CozySurface = Surface
val CozyPink = PrimaryContainer
val CozyRose = Primary
val CozyCherry = SecondaryContainer
val CozyCocoa = OnSurface
val CozyMuted = OnSurfaceVariant
val CozyTerracotta = Tertiary
val CozyTerracottaSoft = TertiaryContainer
val CozyBorder = OutlineVariant

@Composable
fun CozyPage(
    modifier: Modifier = Modifier,
    decorative: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (decorative) {
            CozyDecorations()
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 920.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun CozyDecorations() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(Color(0xFFFFD1DC).copy(alpha = 0.12f), size.width * 0.28f, Offset(size.width * 0.08f, size.height * 0.10f))
        drawCircle(Color(0xFFF8A98E).copy(alpha = 0.08f), size.width * 0.24f, Offset(size.width * 0.93f, size.height * 0.28f))
        drawCircle(Color(0xFFF4A7B9).copy(alpha = 0.10f), size.width * 0.20f, Offset(size.width * 0.18f, size.height * 0.78f))
        drawHeart(Offset(size.width * 0.80f, size.height * 0.12f), 32f, Color(0xFFF4A7B9).copy(alpha = 0.12f))
        drawHeart(Offset(size.width * 0.10f, size.height * 0.52f), 24f, Color(0xFF8B4E38).copy(alpha = 0.06f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
    topLeft: Offset,
    sizePx: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(topLeft.x + sizePx * 0.50f, topLeft.y + sizePx * 0.92f)
        cubicTo(topLeft.x - sizePx * 0.05f, topLeft.y + sizePx * 0.50f, topLeft.x + sizePx * 0.02f, topLeft.y + sizePx * 0.08f, topLeft.x + sizePx * 0.30f, topLeft.y + sizePx * 0.12f)
        cubicTo(topLeft.x + sizePx * 0.42f, topLeft.y + sizePx * 0.14f, topLeft.x + sizePx * 0.49f, topLeft.y + sizePx * 0.24f, topLeft.x + sizePx * 0.50f, topLeft.y + sizePx * 0.32f)
        cubicTo(topLeft.x + sizePx * 0.51f, topLeft.y + sizePx * 0.24f, topLeft.x + sizePx * 0.58f, topLeft.y + sizePx * 0.14f, topLeft.x + sizePx * 0.70f, topLeft.y + sizePx * 0.12f)
        cubicTo(topLeft.x + sizePx * 0.98f, topLeft.y + sizePx * 0.08f, topLeft.x + sizePx * 1.05f, topLeft.y + sizePx * 0.50f, topLeft.x + sizePx * 0.50f, topLeft.y + sizePx * 0.92f)
        close()
    }
    drawPath(path, color)
}

@Composable
fun CozyTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leadingIcon != null) {
            Surface(shape = CircleShape, color = SecondaryContainer.copy(alpha = 0.7f)) {
                Icon(leadingIcon, contentDescription = null, tint = Primary, modifier = Modifier.padding(9.dp).size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CozyCocoa,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = CozyMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun CozyMainTopBar(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = CozySurface.copy(alpha = 0.94f)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(72.dp)
            .background(containerColor)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.CenterStart) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = CozyRose.copy(alpha = 0.82f),
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = title,
            color = CozyRose,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.CenterEnd) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "通知",
                tint = CozyMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CozyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = CozySurface.copy(alpha = 0.90f),
    borderColor: Color = CozyBorder.copy(alpha = 0.72f),
    radius: Int = 28,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.975f else 1f, tween(140), label = "cozyCardScale")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(radius.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.82f))
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun CozyPill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    color: Color = Primary,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) color else color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = if (selected) 0f else 0.22f))
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFFFFFCF8) else color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun CozyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "cozyButtonScale")
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        interactionSource = interaction,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color(0xFFFFFCF8),
            disabledContainerColor = Primary.copy(alpha = 0.45f),
            disabledContentColor = Color(0xFFFFFCF8).copy(alpha = 0.8f)
        )
    ) {
        Text(text, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun CozyIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    background: Color = SecondaryContainer.copy(alpha = 0.62f),
    tint: Color = Primary
) {
    Surface(modifier = modifier.size(44.dp), shape = RoundedCornerShape(16.dp), color = background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
fun cozyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFFFFCF8),
    unfocusedContainerColor = SurfaceContainerLow.copy(alpha = 0.88f),
    focusedBorderColor = PrimaryContainer,
    unfocusedBorderColor = OutlineVariant.copy(alpha = 0.72f),
    cursorColor = Primary,
    focusedLabelColor = Primary,
    unfocusedLabelColor = OnSurfaceVariant
)
