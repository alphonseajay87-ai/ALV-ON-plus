package com.alveon.plus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import com.alveon.plus.ui.theme.Palette

/**
 * The one focusable building block used across the app (Compose for TV Surface):
 * scales up and gets a silver outline when focused. [content] receives the focus state.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    container: Color = Palette.Surface,
    focusedContainer: Color = Palette.SurfaceHigh,
    focusedScale: Float = 1.05f,
    onFocus: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            focused = it.hasFocus
            if (it.hasFocus) onFocus?.invoke()
        },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = Palette.Silver,
            focusedContainerColor = focusedContainer,
            focusedContentColor = Palette.Silver,
            pressedContainerColor = focusedContainer,
            pressedContentColor = Palette.Silver,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, Palette.Silver), shape = shape),
        ),
    ) {
        this.content(focused)
    }
}

/** Pill-shaped text button. Inverts to black-on-silver when focused. */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    destructive: Boolean = false,
    glyph: GlyphKind? = null,
    onFocus: (() -> Unit)? = null,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        container = if (selected) Palette.Selected else Palette.Surface,
        focusedContainer = Palette.Silver,
        focusedScale = 1.06f,
        onFocus = onFocus,
    ) { focused ->
        val tint = when {
            focused -> Palette.Black
            destructive -> Palette.Danger
            else -> Palette.Silver
        }
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (glyph != null) Glyph(glyph, tint, Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}

/** Round, transparent icon button for the player and top bars. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    content: @Composable (tint: Color) -> Unit,
) {
    FocusSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier.size(size),
        shape = CircleShape,
        container = Color.Transparent,
        focusedContainer = Palette.Silver,
        focusedScale = 1.12f,
    ) { focused ->
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = if (enabled) 1f else 0.35f },
            contentAlignment = Alignment.Center,
        ) {
            content(if (focused) Palette.Black else Palette.Silver)
        }
    }
}
