package com.alveon.plus.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alveon.plus.AppGraph
import com.alveon.plus.ui.components.AlveonWordmark
import com.alveon.plus.ui.components.FocusSurface
import com.alveon.plus.ui.components.Glyph
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.MediaCard
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.components.glass
import com.alveon.plus.ui.components.rememberFolderPicker
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay

private enum class HomeTab(val label: String) {
    ForYou("For You"), Movies("Movies"), Series("Series")
}

private data class RowSpec(val title: String, val items: List<HomeItem>, val wide: Boolean = false)

@Composable
fun HomeScreen(
    onOpen: (HomeItem) -> Unit,
    onEpisodes: (HomeItem) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val locations by vm.locations.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val addFolder = rememberFolderPicker()

    LaunchedEffect(locations, servers) { vm.refresh() }

    val (movies, series) = remember(library) { groupLibrary(library) }
    val continueItems = remember(progress) { progress.map { it.toHomeItem() } }
    val myList = remember(favorites, movies, series) { (movies + series).filter { it.key in favorites } }
    val recent = remember(library) { recentlyAdded(library) }

    var tab by remember { mutableStateOf(HomeTab.ForYou) }
    val rows = remember(tab, continueItems, myList, movies, series, recent) {
        when (tab) {
            HomeTab.ForYou -> listOf(
                RowSpec("Continue Watching", continueItems, wide = true),
                RowSpec("Recently Added", recent),
                RowSpec("My List", myList),
                RowSpec("Movies", movies.take(30)),
                RowSpec("Series", series.take(30)),
            )
            HomeTab.Movies -> listOf(
                RowSpec("Recently Added", recent.filter { !it.isSeries }),
                RowSpec("All Movies", movies),
                RowSpec("Top Rated", movies.filter { !it.info?.rating.isNullOrBlank() }
                    .sortedByDescending { it.info?.rating?.toDoubleOrNull() ?: 0.0 }.take(20)),
            )
            HomeTab.Series -> listOf(RowSpec("All Series", series))
        }.filter { it.items.isNotEmpty() }
    }

    var hero by remember { mutableStateOf<HomeItem?>(null) }
    val shown = hero?.takeIf { h -> rows.any { r -> r.items.any { it.key == h.key } } }
        ?: rows.firstOrNull()?.items?.firstOrNull()

    val firstFocus = remember { FocusRequester() }
    var focusedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(rows.isNotEmpty()) {
        if (rows.isNotEmpty() && !focusedOnce) {
            delay(250)
            runCatching { firstFocus.requestFocus() }
            focusedOnce = true
        }
    }

    val hasSources = locations.isNotEmpty() || servers.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(shown?.info?.backdropUrl ?: shown?.info?.posterUrl)

        Column(Modifier.fillMaxSize()) {
            TopBar(tab = tab, onTab = { tab = it }, onSearch = onSearch, onSettings = onSettings)

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                Crossfade(targetState = shown, animationSpec = tween(350), label = "hero-info") { item ->
                    Box(Modifier.padding(start = 40.dp, bottom = 6.dp)) {
                        if (item != null) {
                            HeroCard(
                                item = item,
                                isFav = item.favKey in favorites,
                                onPrimary = { onOpen(item) },
                                onEpisodes = { onEpisodes(item) },
                                onFav = { AppGraph.store.toggleFavorite(item.favKey) },
                            )
                        } else {
                            WelcomeCard(hasSources = hasSources, onAddFolder = addFolder, onSettings = onSettings)
                        }
                    }
                }
            }

            LazyColumn(Modifier.fillMaxWidth().height(214.dp)) {
                rows.forEachIndexed { rowIndex, spec ->
                    item(key = spec.title) {
                        HomeRow(
                            spec = spec,
                            firstFocus = if (rowIndex == 0) firstFocus else null,
                            onFocus = { hero = it },
                            onOpen = onOpen,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroBackdrop(url: String?) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF14141A), Color.Black)))) {
        Crossfade(targetState = url, animationSpec = tween(800), label = "backdrop") { u ->
            if (u != null) {
                var started by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { started = true }
                val zoom by animateFloatAsState(
                    targetValue = if (started) 1.07f else 1f,
                    animationSpec = tween(16_000, easing = LinearEasing),
                    label = "ken-burns",
                )
                AsyncImage(
                    model = u,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom },
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xB3000000), Color(0x40000000), Color.Transparent)),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x66000000),
                    0.30f to Color.Transparent,
                    0.62f to Color(0x80000000),
                    1f to Color(0xF2000000),
                ),
            ),
        )
    }
}

@Composable
private fun TopBar(tab: HomeTab, onTab: (HomeTab) -> Unit, onSearch: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 18.dp)) {
        Row(
            Modifier.align(Alignment.Center).glass(CircleShape).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTab.values().forEach { t ->
                FocusSurface(
                    onClick = { onTab(t) },
                    onFocus = { onTab(t) },
                    shape = CircleShape,
                    container = if (t == tab) Color(0x40FFFFFF) else Color.Transparent,
                    focusedContainer = Palette.Silver,
                    focusedScale = 1.04f,
                ) { focused ->
                    Text(
                        t.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (focused) Palette.Black else Palette.Silver,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                    )
                }
            }
            IconTab(GlyphKind.Search, onSearch)
            IconTab(GlyphKind.Gear, onSettings)
        }
        AlveonWordmark(Modifier.align(Alignment.CenterEnd), logoSize = 34.dp)
    }
}

@Composable
private fun IconTab(glyph: GlyphKind, onClick: () -> Unit) {
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = Color.Transparent,
        focusedContainer = Palette.Silver,
        focusedScale = 1.06f,
    ) { focused ->
        Glyph(
            glyph,
            if (focused) Palette.Black else Palette.Silver,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp).size(20.dp),
        )
    }
}

@Composable
private fun HeroCard(
    item: HomeItem,
    isFav: Boolean,
    onPrimary: () -> Unit,
    onEpisodes: () -> Unit,
    onFav: () -> Unit,
) {
    val info = item.info
    val allProgress by AppGraph.store.progress.collectAsStateWithLifecycle()
    val next = if (item.isSeries) remember(item, allProgress) { nextEpisodeFor(item) } else null
    val resumingNext = next != null && allProgress.any { it.uri == next.entry.uri }
    val nextTag = next?.let { "S${it.parsed.season ?: 1} · E${it.parsed.episode ?: 1}" }
    val kicker = when {
        item.isContinue -> "CONTINUE WATCHING"
        item.isSeries -> "SERIES"
        else -> "MOVIE"
    }
    val meta = listOfNotNull(
        info?.year?.ifBlank { null },
        info?.rating?.ifBlank { null }?.let { "★ $it" },
        if (item.isSeries) "${item.episodes.size} episodes" else null,
    ).joinToString("  ·  ")

    Column(
        Modifier.width(430.dp).glass().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(kicker, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp), color = Palette.Muted)
        Text(
            item.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = Palette.Silver)
        val overview = info?.overview.orEmpty()
        if (overview.isNotBlank()) {
            Text(
                overview,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = .8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvButton(
                text = when {
                    item.isSeries -> (if (resumingNext) "Resume " else "Play ") + nextTag.orEmpty()
                    item.isContinue -> "Resume"
                    else -> "Play"
                },
                onClick = onPrimary,
                glyph = GlyphKind.Play,
                selected = true,
            )
            if (item.isSeries) TvButton("Episodes", onEpisodes)
            TvButton(
                text = if (isFav) "In My List" else "My List",
                onClick = onFav,
                glyph = if (isFav) GlyphKind.Check else GlyphKind.Plus,
            )
        }
    }
}

@Composable
private fun WelcomeCard(hasSources: Boolean, onAddFolder: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.width(430.dp).glass().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasSources) {
            Text("Looking for your movies…", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Your library appears here as soon as videos are found. If nothing shows up, check the folder or server in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted,
            )
            TvButton("Settings", onSettings, Modifier.padding(top = 6.dp))
        } else {
            Text("Welcome to ALVéON+", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Add a USB drive or folder to see your movies and series here. You can also connect a home server in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted,
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton("Add USB / folder", onAddFolder, selected = true, glyph = GlyphKind.Plus)
                TvButton("Settings", onSettings)
            }
        }
    }
}

@Composable
private fun HomeRow(
    spec: RowSpec,
    firstFocus: FocusRequester?,
    onFocus: (HomeItem) -> Unit,
    onOpen: (HomeItem) -> Unit,
) {
    Column {
        Text(
            spec.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 40.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(spec.items, key = { _, it -> it.key }) { index, item ->
                val image = if (spec.wide) item.info?.backdropUrl ?: item.info?.posterUrl
                else item.info?.posterUrl ?: item.info?.backdropUrl
                MediaCard(
                    title = item.title,
                    imageUrl = image,
                    width = if (spec.wide) 190.dp else 104.dp,
                    aspect = if (spec.wide) 16f / 9f else 2f / 3f,
                    progress = item.progress,
                    modifier = if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                    onFocus = { onFocus(item) },
                    onClick = { onOpen(item) },
                )
            }
        }
    }
}
