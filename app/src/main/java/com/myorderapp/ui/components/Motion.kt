package com.myorderapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay

object CozyMotion {
    const val Quick = 120
    const val Exit = 140
    const val Toast = 160
    const val Standard = 220
    const val Reveal = 280
    const val Slow = 420
    const val CartFly = 620

    const val PressedScale = 0.96f
    const val SoftPressedScale = 0.985f
    const val ButtonPressedScale = 0.97f

    fun fadeUp(offset: Int = 18, durationMillis: Int = Reveal): EnterTransition =
        fadeIn(tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)) { offset }
}

@Composable
fun CozyMotionVisibility(
    modifier: Modifier = Modifier,
    enter: EnterTransition = CozyMotion.fadeUp(),
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = enter, modifier = modifier) {
        content()
    }
}

@Composable
fun Modifier.cozyPulseOnChange(value: Any?, targetScale: Float = 1.08f): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(value) {
        scale.snapTo(1f)
        scale.animateTo(targetScale, tween(CozyMotion.Quick, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(CozyMotion.Standard, easing = FastOutSlowInEasing))
    }
    return this.scale(scale.value)
}
