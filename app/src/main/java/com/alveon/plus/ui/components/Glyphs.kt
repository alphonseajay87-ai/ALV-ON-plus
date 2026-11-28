package com.alveon.plus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

enum class GlyphKind { Play, Pause, Folder, Server, Plus, Back, Prev, Next, Speaker, Gear, Subtitles, Search, Check }

/** Small vector icons drawn in code, so the app needs no icon library. */
@Composable
fun Glyph(kind: GlyphKind, color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val round = Stroke(width = w * .09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            GlyphKind.Play -> drawPath(
                Path().apply {
                    moveTo(w * .28f, h * .15f); lineTo(w * .86f, h * .5f); lineTo(w * .28f, h * .85f); close()
                },
                color,
            )
            GlyphKind.Pause -> {
                drawRect(color, Offset(w * .22f, h * .15f), Size(w * .20f, h * .70f))
                drawRect(color, Offset(w * .58f, h * .15f), Size(w * .20f, h * .70f))
            }
            GlyphKind.Folder -> {
                drawRoundRect(color, Offset(w * .08f, h * .16f), Size(w * .36f, h * .24f), CornerRadius(w * .05f))
                drawRoundRect(color, Offset(w * .08f, h * .28f), Size(w * .84f, h * .58f), CornerRadius(w * .07f))
            }
            GlyphKind.Server -> {
                for (i in 0..2) {
                    drawRoundRect(
                        color, Offset(w * .10f, h * (.10f + .30f * i)), Size(w * .80f, h * .22f),
                        CornerRadius(w * .05f), style = Stroke(width = w * .06f),
                    )
                }
            }
            GlyphKind.Plus -> {
                drawRect(color, Offset(w * .44f, h * .16f), Size(w * .12f, h * .68f))
                drawRect(color, Offset(w * .16f, h * .44f), Size(w * .68f, h * .12f))
            }
            GlyphKind.Back -> drawPath(
                Path().apply {
                    moveTo(w * .65f, h * .15f); lineTo(w * .30f, h * .50f); lineTo(w * .65f, h * .85f)
                },
                color,
                style = Stroke(width = w * .10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            GlyphKind.Prev -> {
                drawRect(color, Offset(w * .18f, h * .2f), Size(w * .09f, h * .6f))
                drawPath(
                    Path().apply { moveTo(w * .82f, h * .2f); lineTo(w * .36f, h * .5f); lineTo(w * .82f, h * .8f); close() },
                    color,
                )
            }
            GlyphKind.Next -> {
                drawRect(color, Offset(w * .73f, h * .2f), Size(w * .09f, h * .6f))
                drawPath(
                    Path().apply { moveTo(w * .18f, h * .2f); lineTo(w * .64f, h * .5f); lineTo(w * .18f, h * .8f); close() },
                    color,
                )
            }
            GlyphKind.Speaker -> {
                drawPath(
                    Path().apply {
                        moveTo(w * .12f, h * .38f); lineTo(w * .30f, h * .38f); lineTo(w * .50f, h * .20f)
                        lineTo(w * .50f, h * .80f); lineTo(w * .30f, h * .62f); lineTo(w * .12f, h * .62f); close()
                    },
                    color,
                )
                drawArc(
                    color, -50f, 100f, false, Offset(w * .30f, h * .30f), Size(w * .40f, h * .40f),
                    style = round,
                )
                drawArc(
                    color, -50f, 100f, false, Offset(w * .22f, h * .18f), Size(w * .60f, h * .64f),
                    style = round,
                )
            }
            GlyphKind.Gear -> {
                val c = Offset(w / 2, h / 2)
                drawCircle(color, w * .17f, c, style = Stroke(width = w * .09f))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val d = Offset(cos(a).toFloat(), sin(a).toFloat())
                    drawLine(color, c + d * (w * .27f), c + d * (w * .40f), strokeWidth = w * .11f, cap = StrokeCap.Round)
                }
                drawCircle(color, w * .27f, c, style = Stroke(width = w * .07f))
            }
            GlyphKind.Subtitles -> {
                drawRoundRect(color, Offset(w * .10f, h * .18f), Size(w * .80f, h * .56f), CornerRadius(w * .12f), style = round)
                drawPath(
                    Path().apply { moveTo(w * .34f, h * .74f); lineTo(w * .28f, h * .90f); lineTo(w * .50f, h * .74f) },
                    color, style = round,
                )
                drawLine(color, Offset(w * .26f, h * .38f), Offset(w * .46f, h * .38f), strokeWidth = w * .07f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * .54f, h * .38f), Offset(w * .74f, h * .38f), strokeWidth = w * .07f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * .26f, h * .54f), Offset(w * .40f, h * .54f), strokeWidth = w * .07f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * .48f, h * .54f), Offset(w * .74f, h * .54f), strokeWidth = w * .07f, cap = StrokeCap.Round)
            }
            GlyphKind.Search -> {
                drawCircle(color, w * .26f, Offset(w * .43f, h * .43f), style = round)
                drawLine(color, Offset(w * .63f, h * .63f), Offset(w * .86f, h * .86f), strokeWidth = w * .10f, cap = StrokeCap.Round)
            }
            GlyphKind.Check -> drawPath(
                Path().apply { moveTo(w * .2f, h * .52f); lineTo(w * .42f, h * .72f); lineTo(w * .80f, h * .28f) },
                color, style = Stroke(width = w * .11f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** The circular "skip 10 seconds" arrow with a 10 in the middle. */
@Composable
fun SkipGlyph(forward: Boolean, tint: Color, iconSize: Dp = 30.dp) {
    Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(iconSize).graphicsLayer { scaleX = if (forward) 1f else -1f }) {
            val w = iconSize.toPx()
            val c = Offset(w / 2, w / 2)
            val r = w * .38f
            drawArc(
                tint, startAngle = -60f, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(width = w * .08f, cap = StrokeCap.Round),
            )
            // arrow head at the end of the arc (11 o'clock), pointing clockwise
            val a = Math.toRadians(240.0)
            val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            val dir = Offset((-sin(a)).toFloat(), cos(a).toFloat())
            val n = Offset(-dir.y, dir.x)
            drawPath(
                Path().apply {
                    moveTo(p.x + dir.x * r * .5f, p.y + dir.y * r * .5f)
                    lineTo(p.x + n.x * r * .32f, p.y + n.y * r * .32f)
                    lineTo(p.x - n.x * r * .32f, p.y - n.y * r * .32f)
                    close()
                },
                tint,
            )
        }
        Text("10", style = TextStyle(color = tint, fontSize = (iconSize.value * .30f).sp, fontWeight = FontWeight.SemiBold))
    }
}
