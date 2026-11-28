package com.alveon.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alveon.plus.ui.theme.Palette

private val CardShape = RoundedCornerShape(14.dp)

/**
 * A poster (or wide backdrop) card. The artwork is the whole card; titles live in the hero
 * area above the rows. Without artwork it shows the title on a soft gradient.
 */
@Composable
fun MediaCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    width: Dp = 108.dp,
    aspect: Float = 2f / 3f,
    progress: Float? = null,
    onFocus: (() -> Unit)? = null,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.width(width).aspectRatio(aspect),
        shape = CardShape,
        focusedScale = 1.08f,
        onFocus = onFocus,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CardShape),
            )
        } else {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A2A2F), Color(0xFF111113)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
        if (progress != null) {
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                    .background(Color.White.copy(alpha = .28f)),
            ) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Palette.Silver))
            }
        }
    }
}
