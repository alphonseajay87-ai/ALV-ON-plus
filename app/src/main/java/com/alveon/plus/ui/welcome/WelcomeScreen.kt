package com.alveon.plus.ui.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alveon.plus.R
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.Glyph
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay

/** One-time, short explanation of the app. */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(700))
        delay(100)
        runCatching { focus.requestFocus() }
    }

    Column(
        Modifier.fillMaxSize().graphicsLayer { this.alpha = fade.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(14.dp))
        Text("Welcome to ALVéON+", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your movies and series, beautifully organized. Free, with no account.",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 520.dp),
        )
        Spacer(Modifier.height(24.dp))
        Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Point(GlyphKind.Folder, "Add your videos", "Pick a USB drive or folder on the home screen. Home servers (web folder or WebDAV) are set up in Settings.")
            Point(GlyphKind.Play, "Posters appear by themselves", "Titles are matched online with TheMovieDB. Series are grouped into episodes.")
            Point(GlyphKind.Subtitles, "Made for the remote", "Press OK to pause and see the story. Audio tracks and subtitles are one press away.")
        }
        Spacer(Modifier.height(26.dp))
        TvButton("Get started", onContinue, Modifier.focusRequester(focus), selected = true)
    }
}

@Composable
private fun Point(glyph: GlyphKind, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(34.dp).background(Color(0x22FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Glyph(glyph, Palette.Silver, Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        }
    }
}
