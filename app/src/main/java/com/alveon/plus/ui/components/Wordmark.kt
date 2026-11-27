package com.alveon.plus.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alveon.plus.R
import com.alveon.plus.ui.theme.Palette

/** Logo + "ALVéON+" with a soft light sweep across the letters every few seconds. */
@Composable
fun AlveonWordmark(
    modifier: Modifier = Modifier,
    textSize: TextUnit = 16.sp,
    logoSize: Dp = 24.dp,
) {
    val transition = rememberInfiniteTransition(label = "wordmark")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = keyframes<Float> {
                durationMillis = 7000
                0f at 0
                1f at 1600 using FastOutSlowInEasing
                1f at 7000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val textWidthPx = with(LocalDensity.current) { textSize.toPx() * 6.6f }
    val centerX = -textWidthPx * 0.3f + sweep * textWidthPx * 1.6f
    val brush = Brush.linearGradient(
        colors = listOf(Palette.Silver.copy(alpha = .72f), Color.White, Palette.Silver.copy(alpha = .72f)),
        start = Offset(centerX - textWidthPx * 0.22f, 0f),
        end = Offset(centerX + textWidthPx * 0.22f, 0f),
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(logoSize))
        Spacer(Modifier.width(8.dp))
        BasicText(
            "ALVéON+",
            style = TextStyle(brush = brush, fontSize = textSize, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp),
        )
    }
}
