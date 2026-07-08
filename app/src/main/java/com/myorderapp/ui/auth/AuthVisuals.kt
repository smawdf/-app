package com.myorderapp.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myorderapp.R
import com.myorderapp.ui.theme.OnBackground
import com.myorderapp.ui.theme.OnSurfaceVariant
import com.myorderapp.ui.theme.OutlineVariant
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.PrimaryContainer
import com.myorderapp.ui.theme.Surface
import com.myorderapp.ui.theme.SurfaceVariant

val AuthInk = OnBackground
val AuthMuted = OnSurfaceVariant
val AuthPrimaryStart = Color(0xFFFFA7BC)
val AuthPrimaryEnd = Primary
val AuthField = Surface
val AuthFieldStroke = OutlineVariant
private val AuthCream = Color(0xFFFFE8D3)
private val AuthPinkMist = PrimaryContainer
private val AuthHeart = Color(0xFFEFA7B5)
private val AuthSurfaceVariant = SurfaceVariant

@Composable
fun AuthDecoratedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFEF8F2))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(AuthCream.copy(alpha = 0.16f), radius = size.width * 0.22f, center = Offset(size.width * 0.05f, size.height * 0.12f))
            drawCircle(AuthPinkMist.copy(alpha = 0.10f), radius = size.width * 0.17f, center = Offset(size.width * 0.86f, size.height * 0.24f))
            drawCircle(AuthPinkMist.copy(alpha = 0.08f), radius = size.width * 0.24f, center = Offset(size.width * 0.86f, size.height * 0.80f))
            drawCircle(AuthCream.copy(alpha = 0.14f), radius = size.width * 0.16f, center = Offset(size.width * 0.13f, size.height * 0.68f))

            drawCircle(AuthPrimaryStart.copy(alpha = 0.09f), radius = 4.dp.toPx(), center = Offset(size.width * 0.73f, size.height * 0.22f))
            drawCircle(AuthPrimaryEnd.copy(alpha = 0.07f), radius = 2.5.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.20f))
            drawCircle(AuthHeart.copy(alpha = 0.08f), radius = 3.dp.toPx(), center = Offset(size.width * 0.88f, size.height * 0.15f))

            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.30f)
                    cubicTo(size.width * 0.25f, size.height * 0.24f, size.width * 0.42f, size.height * 0.25f, size.width * 0.55f, size.height * 0.31f)
                    cubicTo(size.width * 0.68f, size.height * 0.37f, size.width * 0.80f, size.height * 0.37f, size.width * 0.94f, size.height * 0.30f)
                },
                color = AuthPrimaryStart.copy(alpha = 0.08f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 11.dp.toPx())))
            )
            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.72f)
                    cubicTo(size.width * 0.25f, size.height * 0.67f, size.width * 0.45f, size.height * 0.68f, size.width * 0.58f, size.height * 0.74f)
                    cubicTo(size.width * 0.72f, size.height * 0.80f, size.width * 0.85f, size.height * 0.80f, size.width * 0.95f, size.height * 0.73f)
                },
                color = AuthPrimaryStart.copy(alpha = 0.07f),
                style = Stroke(width = 1.4.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 10.dp.toPx())))
            )

            drawHeart(Offset(size.width * 0.86f, size.height * 0.18f), 11.dp.toPx(), AuthHeart.copy(alpha = 0.10f))
            drawHeart(Offset(size.width * 0.14f, size.height * 0.56f), 9.dp.toPx(), AuthHeart.copy(alpha = 0.08f))
            drawHeart(Offset(size.width * 0.88f, size.height * 0.52f), 8.dp.toPx(), AuthHeart.copy(alpha = 0.08f), outline = true)
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
    topLeft: Offset,
    sizePx: Float,
    color: Color,
    outline: Boolean = false
) {
    translate(left = topLeft.x, top = topLeft.y) {
        val path = Path().apply {
            moveTo(sizePx * 0.50f, sizePx * 0.90f)
            cubicTo(sizePx * -0.05f, sizePx * 0.48f, sizePx * 0.02f, sizePx * 0.08f, sizePx * 0.30f, sizePx * 0.12f)
            cubicTo(sizePx * 0.42f, sizePx * 0.14f, sizePx * 0.49f, sizePx * 0.23f, sizePx * 0.50f, sizePx * 0.31f)
            cubicTo(sizePx * 0.51f, sizePx * 0.23f, sizePx * 0.58f, sizePx * 0.14f, sizePx * 0.70f, sizePx * 0.12f)
            cubicTo(sizePx * 0.98f, sizePx * 0.08f, sizePx * 1.05f, sizePx * 0.48f, sizePx * 0.50f, sizePx * 0.90f)
            close()
        }
        if (outline) {
            drawPath(path, color, style = Stroke(width = 1.4.dp.toPx()))
        } else {
            drawPath(path, color)
        }
    }
}

@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(128.dp)
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(40.dp))
            .padding(4.dp)
            .clip(RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_orderdisk_dogs_cropped),
            contentDescription = "应用图标",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AuthGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Surface.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, AuthFieldStroke.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Box(Modifier.padding(24.dp)) { content() } }
    )
}

@Composable
fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
    floatingLabel: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(if (supportingText == null) 60.dp else 82.dp),
        label = if (floatingLabel) {
            { Text(label, color = AuthInk, fontWeight = FontWeight.Bold) }
        } else {
            null
        },
        placeholder = { Text(placeholder, color = AuthMuted.copy(alpha = 0.72f)) },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon ?: if (isPassword) Icons.Outlined.Lock else Icons.Outlined.Person,
                contentDescription = null,
                tint = AuthPrimaryEnd
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        tint = AuthPrimaryEnd
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFFFF4F0).copy(alpha = 0.72f),
            focusedBorderColor = AuthPinkMist,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = AuthPrimaryEnd,
            focusedLabelColor = AuthInk,
            unfocusedLabelColor = AuthInk
        ),
        supportingText = supportingText?.let {
            { Text(it, color = AuthPrimaryEnd.copy(alpha = 0.82f), textAlign = TextAlign.End) }
        }
    )
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(60.dp)
            .scale(if (pressed) 0.98f else 1f)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF9FB7),
            disabledContainerColor = AuthFieldStroke.copy(alpha = 0.70f)
        )
        ) {
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AuthBottomLink(
    prefix: String,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prefix, color = AuthMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = onClick) {
            Text(actionText, color = AuthPrimaryEnd, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
