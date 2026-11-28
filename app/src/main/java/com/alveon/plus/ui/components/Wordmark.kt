package com.alveon.plus.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alveon.plus.R

/**
 * A soft band of light that passes across whatever it is applied to, only where the content
 * has pixels (so on the logo it glints across the shape, not the empty space around it).
 * [progress] runs 0..1; at 0 and 1 the band is off-screen.
 */
fun Modifier.lightSweep(progress: Float, strength: Float = 0.6f): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (progress > 0f && progress < 1f) {
            val w = size.width
            val center = -0.4f * w + progress * 1.8f * w
            val half = 0.28f * w
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
                    start = Offset(center - half, 0f),
                    end = Offset(center + half, size.height * 0.35f),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        }
    }

/** Just the ALVéON+ mark, with a quiet glint passing over it every few seconds. */
@Composable
fun AlveonWordmark(
    modifier: Modifier = Modifier,
    logoSize: Dp = 30.dp,
) {
    val transition = rememberInfiniteTransition(label = "logo-glint")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = keyframes<Float> {
                durationMillis = 8000
                0f at 0
                1f at 1400 using FastOutSlowInEasing
                1f at 8000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "glint",
    )
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = "ALVéON+",
        modifier = modifier.size(logoSize).lightSweep(sweep, strength = 0.5f),
    )
}
