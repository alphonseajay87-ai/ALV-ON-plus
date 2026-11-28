package com.alveon.plus.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alveon.plus.R
import com.alveon.plus.ui.components.lightSweep
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.math.pow

// ---- timeline in milliseconds
private const val OPEN = 1000f   // the moment the line of light opens and the mark appears
private const val TOTAL = 2800f

private fun seg(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)
private fun smooth(x: Float) = FastOutSlowInEasing.transform(x)

/**
 * Silent, minimal intro (~2.8 s):
 *  0.0 – 1.0 s   black. One thin line of light appears in the centre and grows.
 *  1.0 s         the line opens like a curtain of light and the mark rises out of it,
 *                softly, with a faint halo behind it.
 *  1.5 – 2.0 s   a single glint crosses the mark.
 *  2.0 – 2.8 s   the mark pushes toward you, getting large and fading into the app.
 * Any key skips it.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val done = rememberUpdatedState(onFinished)
    val clock = remember { Animatable(0f) }
    val finished = remember { booleanArrayOf(false) }
    val focus = remember { FocusRequester() }

    fun finish() {
        if (finished[0]) return
        finished[0] = true
        done.value()
    }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        clock.animateTo(TOTAL, tween(TOTAL.toInt(), easing = LinearEasing))
        delay(60)
        finish()
    }

    val t = clock.value
    val sweepProgress = seg(t, 1500f, 2050f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Black)
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown) {
                    finish()
                    true
                } else false
            }
            .focusRequester(focus)
            .focusable()
            .drawBehind {
                val tt = clock.value
                val cx = size.width / 2f
                val cy = size.height / 2f

                // 1) the line of light: grows and brightens, then opens and fades on the reveal
                val build = smooth(seg(tt, 200f, OPEN))
                val open = seg(tt, OPEN, OPEN + 600f)
                if (tt > 200f && open < 1f) {
                    val lineW = size.width * (0.04f + 0.30f * build + 0.9f * smooth(open))
                    val lineH = 2.dp.toPx() * (1f + 7f * smooth(open))
                    val a = (0.25f + 0.75f * build) * (1f - open)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = a), Color.Transparent),
                            startX = cx - lineW / 2f,
                            endX = cx + lineW / 2f,
                        ),
                        topLeft = Offset(cx - lineW / 2f, cy - lineH / 2f),
                        size = Size(lineW, lineH),
                    )
                }

                // 2) a soft halo behind the mark
                if (tt >= OPEN) {
                    val h = seg(tt, OPEN, OPEN + 900f)
                    val r = size.minDimension * (0.30f + 0.30f * smooth(h))
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0x40E6E6EB), Color(0x10DCDCE1), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                        alpha = (0.35f + 0.65f * (1f - smooth(h))) * (1f - seg(tt, 2200f, 2700f)),
                    )
                }

                // 3) a faint flash as the line opens
                val f = 1f - seg(tt, OPEN, OPEN + 350f)
                if (tt >= OPEN && f > 0f) drawRect(Color.White.copy(alpha = 0.22f * f * f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    val tt = clock.value
                    val appear = smooth(seg(tt, OPEN, OPEN + 500f))
                    val rise = 0.86f + 0.14f * smooth(seg(tt, OPEN, OPEN + 700f))
                    val drift = 1f + 0.16f * smooth(seg(tt, OPEN + 300f, 2000f))
                    val rush = 1f + 2.8f * seg(tt, 2000f, TOTAL).pow(3)
                    val s = rise * drift * rush
                    scaleX = s
                    scaleY = s
                    alpha = appear * (1f - seg(tt, 2300f, TOTAL))
                }
                .lightSweep(sweepProgress, strength = 0.7f),
        )
    }
}
