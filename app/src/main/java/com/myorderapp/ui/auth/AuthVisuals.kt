package com.myorderapp.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myorderapp.R

private val AuthInk = Color(0xFF173B44)
private val AuthMuted = Color(0xFF6F858B)
private val AuthPrimaryStart = Color(0xFF7FBBD0)
private val AuthPrimaryEnd = Color(0xFF2E7F99)
private val AuthField = Color(0xFFF8FBFB)
private val AuthFieldStroke = Color(0xFFD6E6E8)
private val AuthCream = Color(0xFFFFF1CF)
private val AuthBlueMist = Color(0xFFD7EBF1)
private val AuthHeart = Color(0xFFEFA7B5)

val AuthPrimaryBrush = Brush.linearGradient(
    colors = listOf(AuthPrimaryStart, AuthPrimaryEnd)
)

@Composable
fun AuthDecoratedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8FCFB), Color(0xFFEEF7F7), Color(0xFFFFF7EA))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(AuthCream.copy(alpha = 0.72f), radius = size.width * 0.22f, center = Offset(size.width * 0.14f, size.height * 0.14f))
            drawCircle(AuthBlueMist.copy(alpha = 0.88f), radius = size.width * 0.17f, center = Offset(size.width * 0.82f, size.height * 0.25f))
            drawCircle(AuthBlueMist.copy(alpha = 0.46f), radius = size.width * 0.24f, center = Offset(size.width * 0.80f, size.height * 0.76f))
            drawCircle(AuthCream.copy(alpha = 0.62f), radius = size.width * 0.16f, center = Offset(size.width * 0.17f, size.height * 0.66f))

            drawCircle(AuthPrimaryStart.copy(alpha = 0.38f), radius = 4.dp.toPx(), center = Offset(size.width * 0.73f, size.height * 0.22f))
            drawCircle(AuthPrimaryEnd.copy(alpha = 0.24f), radius = 2.5.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.20f))
            drawCircle(AuthHeart.copy(alpha = 0.32f), radius = 3.dp.toPx(), center = Offset(size.width * 0.88f, size.height * 0.15f))

            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.30f)
                    cubicTo(size.width * 0.25f, size.height * 0.24f, size.width * 0.42f, size.height * 0.25f, size.width * 0.55f, size.height * 0.31f)
                    cubicTo(size.width * 0.68f, size.height * 0.37f, size.width * 0.80f, size.height * 0.37f, size.width * 0.94f, size.height * 0.30f)
                },
                color = AuthPrimaryStart.copy(alpha = 0.34f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 11.dp.toPx())))
            )
            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.72f)
                    cubicTo(size.width * 0.25f, size.height * 0.67f, size.width * 0.45f, size.height * 0.68f, size.width * 0.58f, size.height * 0.74f)
                    cubicTo(size.width * 0.72f, size.height * 0.80f, size.width * 0.85f, size.height * 0.80f, size.width * 0.95f, size.height * 0.73f)
                },
                color = AuthPrimaryStart.copy(alpha = 0.26f),
                style = Stroke(width = 1.4.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 10.dp.toPx())))
            )

            drawHeart(Offset(size.width * 0.86f, size.height * 0.18f), 11.dp.toPx(), AuthHeart.copy(alpha = 0.50f))
            drawHeart(Offset(size.width * 0.14f, size.height * 0.56f), 9.dp.toPx(), AuthHeart.copy(alpha = 0.34f))
            drawHeart(Offset(size.width * 0.88f, size.height * 0.52f), 8.dp.toPx(), AuthHeart.copy(alpha = 0.28f), outline = true)
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
    Image(
        painter = painterResource(R.drawable.ic_launcher_orderdisk_dogs_cropped),
        contentDescription = "应用图标",
        modifier = modifier.size(118.dp)
    )
}

@Composable
fun AuthGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x332E6474), spotColor = Color(0x222E6474)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Box(Modifier.padding(20.dp)) { content() } }
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
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, color = AuthInk, fontWeight = FontWeight.Bold) },
        placeholder = { Text(placeholder, color = Color(0xFF9AA9AD)) },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = if (isPassword) Icons.Outlined.Lock else Icons.Outlined.Email,
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
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AuthField,
            unfocusedContainerColor = AuthField,
            focusedBorderColor = AuthPrimaryEnd.copy(alpha = 0.72f),
            unfocusedBorderColor = AuthFieldStroke,
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthPrimaryEnd,
            disabledContainerColor = AuthPrimaryEnd.copy(alpha = 0.45f)
        )
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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

@Composable
fun AuthStepPill(currentStep: Int, modifier: Modifier = Modifier) {
    AuthGlassCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthStepBubble(number = 1, label = "账号", active = currentStep >= 1)
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .width(46.dp)
                    .height(2.dp)
                    .background(if (currentStep > 1) AuthPrimaryEnd.copy(alpha = 0.52f) else AuthFieldStroke)
            )
            Spacer(Modifier.width(14.dp))
            AuthStepBubble(number = 2, label = "资料", active = currentStep >= 2)
        }
    }
}

@Composable
private fun AuthStepBubble(number: Int, label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(if (active) AuthPrimaryBrush else Brush.linearGradient(listOf(Color(0xFFEDF4F5), Color(0xFFEDF4F5))), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = if (active) Color.White else Color(0xFF9AABAE),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (active) AuthInk else Color(0xFF9AABAE), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
