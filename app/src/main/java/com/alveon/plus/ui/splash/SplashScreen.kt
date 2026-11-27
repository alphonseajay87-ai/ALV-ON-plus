package com.alveon.plus.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alveon.plus.R
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Black screen. A soft glow blooms, the logo eases in (scale + fade), the wordmark rises
 * underneath while its letters tighten together, then a light sweep runs across it.
 * About 1.9 s in total.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val done = rememberUpdatedState(onFinished)
    val glow = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.86f) }
    val textAlpha = remember { Animatable(0f) }
    val textShift = remember { Animatable(14f) }
    val spacing = remember { Animatable(22f) }
    val sweep = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { glow.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
            launch { logoAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { logoScale.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
            launch { delay(450); textAlpha.animateTo(1f, tween(650)) }
            launch { delay(450); textShift.animateTo(0f, tween(800, easing = FastOutSlowInEasing)) }
            launch { delay(450); spacing.animateTo(9f, tween(1000, easing = FastOutSlowInEasing)) }
            launch { delay(1000); sweep.animateTo(1f, tween(800, easing = LinearEasing)) }
        }
        delay(150)
        done.value()
    }

    val bandX = -200f + sweep.value * 1000f
    val textBrush = Brush.linearGradient(
        colors = listOf(Palette.Silver.copy(alpha = .8f), Color.White, Palette.Silver.copy(alpha = .8f)),
        start = Offset(bandX - 120f, 0f),
        end = Offset(bandX + 120f, 0f),
    )

    Column(
        Modifier.fillMaxSize().background(Palette.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(130.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0x40DCDCE1), Color.Transparent)),
                        radius = size.minDimension * 2.2f * glow.value,
                        alpha = glow.value,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
            )
        }
        Spacer(Modifier.height(18.dp))
        BasicText(
            "ALVéON+",
            style = TextStyle(
                brush = textBrush,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = spacing.value.sp,
            ),
            modifier = Modifier.graphicsLayer {
                alpha = textAlpha.value
                translationY = textShift.value
            },
        )
    }
}
