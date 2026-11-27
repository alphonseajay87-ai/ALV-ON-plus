package com.alveon.plus.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.alveon.plus.AppGraph
import com.alveon.plus.data.LibraryItem
import com.alveon.plus.ui.components.FocusSurface
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.components.glass
import com.alveon.plus.ui.home.groupLibrary
import com.alveon.plus.ui.home.nextEpisodeFor
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay

@Composable
fun SeriesScreen(key: String, onBack: () -> Unit, onPlay: (LibraryItem) -> Unit) {
    val library by AppGraph.library.items.collectAsStateWithLifecycle()
    val progress by AppGraph.store.progress.collectAsStateWithLifecycle()
    val series = remember(library, key) { groupLibrary(library).second.firstOrNull { it.key == key } }
    val episodes = series?.episodes.orEmpty()
    val info = series?.info
    val resume = remember(series, progress) { series?.let { nextEpisodeFor(it) } }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(episodes.isNotEmpty()) {
        if (episodes.isNotEmpty()) {
            delay(200)
            runCatching { playFocus.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val image = info?.backdropUrl ?: info?.posterUrl
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xE6000000), Color(0x99000000), Color(0xCC000000))),
            ),
        )

        Row(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                TvButton("‹  Back", onBack)
                Spacer(Modifier.padding(top = 18.dp))
                Text("SERIES", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                Text(
                    series?.title.orEmpty(),
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = listOfNotNull(
                    info?.year?.ifBlank { null },
                    info?.rating?.ifBlank { null }?.let { "★ $it" },
                    "${episodes.size} episodes",
                ).joinToString("  ·  ")
                Text(meta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                if (!info?.overview.isNullOrBlank()) {
                    Text(
                        info?.overview.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = .8f),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 10.dp).width(360.dp),
                    )
                }
                Spacer(Modifier.padding(top = 16.dp))
                resume?.let { r ->
                    val label = if (progress.any { it.uri == r.entry.uri }) "Resume" else "Play"
                    val tag = r.parsed.let { "S${it.season ?: 1} · E${it.episode ?: 1}" }
                    TvButton(
                        "$label $tag",
                        { onPlay(r) },
                        Modifier.focusRequester(playFocus),
                        selected = true,
                        glyph = GlyphKind.Play,
                    )
                }
            }

            LazyColumn(
                Modifier.width(400.dp).fillMaxHeight().glass(RoundedCornerShape(22.dp)),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(episodes, key = { _, e -> e.entry.uri }) { _, ep ->
                    val watched = progress.firstOrNull { it.uri == ep.entry.uri }
                    FocusSurface(
                        onClick = { onPlay(ep) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        container = Color(0x33000000),
                        focusedScale = 1.02f,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
                            Text(
                                "S${ep.parsed.season ?: 1} · E${ep.parsed.episode ?: 0}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Palette.Muted,
                            )
                            Text(
                                ep.info?.episodeTitle?.ifBlank { null } ?: ep.entry.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (watched != null) {
                                Text(
                                    "${(watched.fraction * 100).toInt()}% watched",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Palette.Silver,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
