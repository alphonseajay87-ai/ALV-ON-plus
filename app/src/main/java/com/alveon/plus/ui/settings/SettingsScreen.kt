package com.alveon.plus.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alveon.plus.AppGraph
import com.alveon.plus.data.ServerEntry
import com.alveon.plus.data.StorageLocation
import com.alveon.plus.ui.components.FocusSurface
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.components.rememberFolderPicker
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddServer: () -> Unit,
    onBrowseFolder: (StorageLocation) -> Unit,
    onBrowseServer: (ServerEntry) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = AppGraph.store
    val servers by store.servers.collectAsStateWithLifecycle()
    val locations by store.locations.collectAsStateWithLifecycle()
    val tmdb by store.tmdbEnabled.collectAsStateWithLifecycle()
    val audioLang by store.preferredAudioLang.collectAsStateWithLifecycle()
    val subLang by store.preferredSubLang.collectAsStateWithLifecycle()
    val langCycle = listOf(
        "" to "Best quality",
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "pt" to "Portuguese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
    )
    val addFolder = rememberFolderPicker()
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvButton("‹  Back", onBack)
                Spacer(Modifier.width(20.dp))
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item { Section("Servers") }
        if (servers.isEmpty()) item { Hint("Connect a home server (web folder or WebDAV) to see its movies on the home screen.") }
        items(servers.size) { i ->
            val s = servers[i]
            RemovableRow(
                title = s.name,
                hint = s.url,
                onOpen = { onBrowseServer(s) },
                onRemove = { store.removeServer(s.id) },
            )
        }
        item { TvButton("Add server", onAddServer, glyph = GlyphKind.Plus, selected = true) }

        item { Section("USB / storage folders") }
        if (locations.isEmpty()) item { Hint("No folders added yet.") }
        items(locations.size) { i ->
            val loc = locations[i]
            RemovableRow(
                title = loc.name,
                hint = "Tap to browse",
                onOpen = { onBrowseFolder(loc) },
                onRemove = { store.removeLocation(loc.treeUri) },
            )
        }
        item { TvButton("Add USB / folder", addFolder, glyph = GlyphKind.Plus, selected = true) }

        item { Section("General") }
        item {
            SettingRow(
                title = "Movie info from TheMovieDB",
                value = if (tmdb) "On" else "Off",
                hint = "Posters and descriptions need internet. Turn off to stay fully offline.",
                onClick = { store.setTmdbEnabled(!tmdb) },
            )
        }

        item {
            SettingRow(
                title = "Preferred audio language",
                value = langCycle.firstOrNull { it.first == audioLang }?.second ?: audioLang.ifBlank { "Best quality" },
                hint = "When set, prefers that language. Otherwise picks the best track (7.1 / Atmos > 5.1 > stereo).",
                onClick = {
                    val idx = langCycle.indexOfFirst { it.first == audioLang }.coerceAtLeast(0)
                    store.setPreferredAudioLang(langCycle[(idx + 1) % langCycle.size].first)
                },
            )
        }
        item {
            SettingRow(
                title = "Preferred subtitle language",
                value = langCycle.firstOrNull { it.first == subLang }?.second
                    ?: if (subLang.isBlank()) "Off / first available" else subLang,
                hint = "Auto-selects this language when the file has matching subtitles (embedded or external).",
                onClick = {
                    val idx = langCycle.indexOfFirst { it.first == subLang }.coerceAtLeast(0)
                    store.setPreferredSubLang(langCycle[(idx + 1) % langCycle.size].first)
                },
            )
        }

        item {
            SettingRow(
                title = "Clear cache",
                value = "Clear",
                hint = "Removes cached posters, movie info and temporary files.",
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { AppGraph.clearCaches() }
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

        item { Section("About ALVéON+") }
        item {
            Hint(
                "ALVéON+ $version\nA free, offline-first media player. No accounts, no ads, no paywalls.\n" +
                    "This product uses the TMDB API but is not endorsed or certified by TMDB."
            )
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp, bottom = 2.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun RemovableRow(title: String, hint: String, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FocusSurface(
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            focusedScale = 1.02f,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hint, style = MaterialTheme.typography.labelMedium, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TvButton("Remove", onRemove, destructive = true)
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit, hint: String? = null) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        focusedScale = 1.02f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (hint != null) {
                    Text(hint, style = MaterialTheme.typography.labelMedium, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
        }
    }
}
