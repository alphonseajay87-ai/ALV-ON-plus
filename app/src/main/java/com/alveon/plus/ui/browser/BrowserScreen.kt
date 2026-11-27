package com.alveon.plus.ui.browser

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alveon.plus.AppGraph
import com.alveon.plus.data.FileNameCleaner
import com.alveon.plus.data.MediaEntry
import com.alveon.plus.data.VideoFormats
import com.alveon.plus.ui.components.FocusSurface
import com.alveon.plus.ui.components.Glyph
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay

private sealed interface BrowseState {
    data object Loading : BrowseState
    data class Loaded(val entries: List<MediaEntry>) : BrowseState
    data class Failed(val message: String) : BrowseState
}

/**
 * Lists one folder. [kind] is "local" (root = SAF tree uri, dir = document uri or empty)
 * or "server" (root = server id, dir = folder URL).
 */
@Composable
fun BrowserScreen(
    kind: String,
    root: String,
    dir: String,
    title: String,
    onBack: () -> Unit,
    onOpenDir: (MediaEntry) -> Unit,
    onPlay: (MediaEntry) -> Unit,
) {
    val context = LocalContext.current
    var state by remember(kind, root, dir) { mutableStateOf<BrowseState>(BrowseState.Loading) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(kind, root, dir, reload) {
        state = BrowseState.Loading
        val result: Result<List<MediaEntry>> = if (kind == "local") {
            runCatching {
                com.alveon.plus.data.StorageBrowser.list(
                    context, Uri.parse(root), dir.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) },
                )
            }
        } else {
            val server = AppGraph.store.serverById(root)
            if (server == null) Result.failure(IllegalStateException("This server was removed."))
            else AppGraph.serverBrowser.list(server, dir)
        }
        state = result.fold(
            onSuccess = { BrowseState.Loaded(it) },
            onFailure = { BrowseState.Failed(it.message ?: "Couldn't load this folder.") },
        )
    }

    Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvButton("‹  Back", onBack)
            Spacer(Modifier.width(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.size(20.dp))

        when (val s = state) {
            BrowseState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Palette.Silver)
            }
            is BrowseState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(s.message, color = Palette.Danger, style = MaterialTheme.typography.bodyLarge)
                TvButton("Try again", { reload++ })
            }
            is BrowseState.Loaded -> if (s.entries.isEmpty()) {
                Text("No folders or videos here.", color = Palette.Muted, style = MaterialTheme.typography.bodyLarge)
            } else {
                val first = remember(s) { FocusRequester() }
                LaunchedEffect(s) {
                    delay(120)
                    runCatching { first.requestFocus() }
                }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(s.entries, key = { _, e -> e.uri }) { index, entry ->
                        EntryRow(
                            entry,
                            modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                            onClick = {
                                if (entry.isDirectory) onOpenDir(entry)
                                else {
                                    AppGraph.queue = s.entries.filter { !it.isDirectory }
                                    onPlay(entry)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: MediaEntry, modifier: Modifier, onClick: () -> Unit) {
    val parsed = if (entry.isDirectory) null else FileNameCleaner.parse(entry.name)
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        focusedScale = 1.02f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(if (entry.isDirectory) GlyphKind.Folder else GlyphKind.Play, Palette.Silver, Modifier.size(26.dp))
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    parsed?.display ?: entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (parsed != null && parsed.display != entry.name) {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (entry.sizeBytes >= 0 && !entry.isDirectory) {
                Spacer(Modifier.width(16.dp))
                Text(VideoFormats.formatSize(entry.sizeBytes), color = Palette.Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
