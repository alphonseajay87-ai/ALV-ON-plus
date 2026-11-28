package com.alveon.plus.ui.player

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.alveon.plus.AppGraph
import com.alveon.plus.data.ExternalSubtitles
import com.alveon.plus.data.FileNameCleaner
import com.alveon.plus.data.MediaEntry
import com.alveon.plus.data.WatchProgress
import com.alveon.plus.ui.components.AlveonWordmark
import com.alveon.plus.ui.components.FocusSurface
import com.alveon.plus.ui.home.seriesKey
import com.alveon.plus.ui.components.GlassIconButton
import com.alveon.plus.ui.components.Glyph
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.SkipGlyph
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.components.glass
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay

private const val SEEK_STEP_MS = 10_000L
private const val INTRO_SKIP_MS = 85_000L
private const val INTRO_WINDOW_END_MS = 240_000L

private enum class Panel { Audio, Subtitles, Speed }

private fun createPlayer(context: Context): ExoPlayer {
    val dataSource = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(AppGraph.httpClient))
    val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
    return ExoPlayer.Builder(context, renderers)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
        .setSeekBackIncrementMs(SEEK_STEP_MS)
        .setSeekForwardIncrementMs(SEEK_STEP_MS)
        .build()
}

@Composable
fun PlayerScreen(
    uri: String,
    name: String,
    onExit: () -> Unit,
    onPlayOther: (MediaEntry) -> Unit,
) {
    val context = LocalContext.current
    val store = AppGraph.store
    val parsed = remember(name) { FileNameCleaner.parse(name) }

    var info by remember(name) { mutableStateOf(AppGraph.tmdb.cached(name)) }
    LaunchedEffect(name) { if (info == null) info = AppGraph.tmdb.lookup(name) }
    val infoState = rememberUpdatedState(info)
    val exit = rememberUpdatedState(onExit)
    val playOther = rememberUpdatedState(onPlayOther)

    // previous / next in the folder or series
    val queue = remember(uri) { AppGraph.queue }
    val index = remember(queue, uri) { queue.indexOfFirst { it.uri == uri } }
    val prevEntry = if (index > 0) queue[index - 1] else null
    val nextEntry = if (index >= 0) queue.getOrNull(index + 1) else null
    val nextState = rememberUpdatedState(nextEntry)

    val player = remember(uri) { createPlayer(context) }

    var isPlaying by remember { mutableStateOf(false) }
    var playWhenReady by remember { mutableStateOf(true) }
    var started by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var trackTick by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintTick by remember { mutableIntStateOf(0) }

    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }

    fun markFinished() {
        if (parsed.isEpisode) store.setLastFinished(seriesKey(parsed.title), uri)
    }

    fun persist() {
        val dur = player.duration
        if (dur == C.TIME_UNSET || dur <= 0) return
        if (player.currentPosition > dur * 0.95) markFinished()
        val i = infoState.value
        store.saveProgress(
            WatchProgress(
                uri = uri,
                fileName = name,
                title = i?.title ?: parsed.title,
                posterUrl = i?.posterUrl,
                backdropUrl = i?.backdropUrl,
                positionMs = player.currentPosition,
                durationMs = dur,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    fun poke() {
        controlsVisible = true
        interactionTick++
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (durationMs > 0) target.coerceAtMost(durationMs) else target)
    }

    fun togglePlay() {
        if (player.playWhenReady) player.pause() else player.play()
    }

    fun goPrevious() {
        if (player.currentPosition > 5_000 || prevEntry == null) player.seekTo(0) else playOther.value(prevEntry)
    }

    // ---- player setup / teardown
    DisposableEffect(player) {
        val resume = (store.progressFor(uri)?.positionMs ?: 0L) - 3_000L
        val externalSubs = ExternalSubtitles.configurations(context, uri)
        val mediaItem = if (externalSubs.isEmpty()) {
            MediaItem.fromUri(uri)
        } else {
            MediaItem.Builder()
                .setUri(uri)
                .setSubtitleConfigurations(externalSubs)
                .build()
        }
        player.setMediaItem(mediaItem, resume.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = true

        var appliedPrefs = false
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
            override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) { playWhenReady = ready }
            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
                // Once tracks are known, pick the best audio (7.1/Atmos > 5.1 > …)
                // and the user's preferred subtitle language.
                if (!appliedPrefs && newTracks.groups.isNotEmpty()) {
                    appliedPrefs = true
                    applyPreferredAudio(player, newTracks, store.preferredAudioLang.value)
                    applyPreferredSubtitle(player, newTracks, store.preferredSubLang.value)
                    trackTick++
                }
            }
            override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) { trackTick++ }
            override fun onPlaybackParametersChanged(parameters: PlaybackParameters) { trackTick++ }
            override fun onPlayerError(e: PlaybackException) {
                error = e.localizedMessage ?: "This file can't be played on this device."
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) started = true
                if (state == Player.STATE_ENDED) {
                    store.removeProgress(uri)
                    markFinished()
                    val next = nextState.value
                    if (next != null) playOther.value(next) else exit.value()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            persist()
            player.removeListener(listener)
            player.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                player.pause()
                persist()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player) {
        var ticks = 0
        while (true) {
            positionMs = player.currentPosition
            durationMs = if (player.duration == C.TIME_UNSET) 0L else player.duration
            if (++ticks % 20 == 0 && player.isPlaying) persist()
            delay(500)
        }
    }

    // ---- what is on screen
    val showPause = started && !playWhenReady && panel == null && error == null
    val showControls = controlsVisible && !showPause && panel == null && error == null

    LaunchedEffect(interactionTick, isPlaying, panel, controlsVisible) {
        if (controlsVisible && isPlaying && panel == null) {
            delay(5_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(showControls, showPause, panel, error) {
        delay(60)
        when {
            panel != null || error != null -> Unit
            showControls -> runCatching { playFocus.requestFocus() }
            else -> runCatching { rootFocus.requestFocus() }
        }
    }
    LaunchedEffect(hintTick) {
        if (hint != null) {
            delay(1_400)
            hint = null
        }
    }

    BackHandler {
        when {
            panel != null -> panel = null
            showControls -> controlsVisible = false
            else -> onExit()
        }
    }

    val title = info?.title ?: parsed.title
    val isSeries = parsed.isEpisode
    val showSkipIntro = isSeries && started && playWhenReady && panel == null && error == null &&
        positionMs in 3_000L..INTRO_WINDOW_END_MS && (durationMs == 0L || durationMs > 600_000L)
    fun skipIntro() {
        val target = player.currentPosition + INTRO_SKIP_MS
        player.seekTo(if (durationMs > 0) target.coerceAtMost(durationMs - 1_000L) else target)
        interactionTick++
    }
    val facts = remember(tracks, trackTick) { videoBadges(tracks) + audioBadges(tracks) }
    val topMeta = listOfNotNull(
        (info?.year?.ifBlank { null } ?: parsed.year),
        info?.rating?.ifBlank { null }?.let { "★ $it" },
        durationMs.takeIf { it > 0 }?.let { formatDuration(it) },
    ) + facts

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown || panel != null || error != null) return@onKeyEvent false
                when (ev.key) {
                    Key.MediaPlayPause -> { togglePlay(); poke(); return@onKeyEvent true }
                    Key.MediaPlay -> { player.play(); poke(); return@onKeyEvent true }
                    Key.MediaPause -> { player.pause(); return@onKeyEvent true }
                    Key.MediaFastForward -> { seekBy(SEEK_STEP_MS); return@onKeyEvent true }
                    Key.MediaRewind -> { seekBy(-SEEK_STEP_MS); return@onKeyEvent true }
                    else -> Unit
                }
                if (showControls) {
                    // Buttons handle their own keys; just keep the controls alive.
                    interactionTick++
                    return@onKeyEvent false
                }
                // Pause screen or hidden controls
                when (ev.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (showSkipIntro) { skipIntro(); return@onKeyEvent true }
                        if (showPause) player.play() else togglePlay()
                        poke()
                        true
                    }
                    Key.DirectionLeft -> {
                        seekBy(-SEEK_STEP_MS)
                        hint = "−10 s   ${formatTime(player.currentPosition)}"; hintTick++
                        true
                    }
                    Key.DirectionRight -> {
                        seekBy(SEEK_STEP_MS)
                        hint = "+10 s   ${formatTime(player.currentPosition)}"; hintTick++
                        true
                    }
                    Key.DirectionUp, Key.DirectionDown, Key.Menu -> { if (!showPause) poke(); true }
                    else -> false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = PlayerView(ctx)
                view.player = player
                view.useController = false
                view.keepScreenOn = true
                view.isFocusable = false
                view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                view.subtitleView?.setFractionalTextSize(0.058f)
                view
            },
        )

        // ---- pause screen: only the story, centered
        AnimatedVisibility(visible = showPause, enter = fadeIn(), exit = fadeOut()) {
            val i = info
            val seriesTitle = (i?.title ?: parsed.title)
            val episodeLine = if (isSeries) {
                val n = parsed.episode
                val epName = i?.episodeTitle.orEmpty()
                when {
                    n != null && epName.isNotBlank() -> "Episode $n: “$epName”"
                    n != null -> "Episode $n"
                    else -> ""
                }
            } else ""
            val kicker = if (isSeries) {
                "SERIES  ·  SEASON ${parsed.season ?: 1}, EPISODE ${parsed.episode ?: 1}"
            } else "MOVIE"
            val overview = if (isSeries) (i?.episodeOverview?.ifBlank { null } ?: i?.overview.orEmpty())
            else i?.overview.orEmpty()
            val details = (listOfNotNull(
                (i?.year?.ifBlank { null } ?: parsed.year),
                i?.rating?.ifBlank { null }?.let { "★ $it" },
                durationMs.takeIf { it > 0 }?.let { formatDuration(it) },
            ) + facts).joinToString("   ·   ")

            Box(
                Modifier.fillMaxSize().background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.widthIn(max = 640.dp).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(kicker, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp), color = Palette.Muted)
                    Text(
                        seriesTitle.uppercase(),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp, letterSpacing = 6.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (episodeLine.isNotBlank()) {
                        Text(episodeLine, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                    }
                    if (details.isNotBlank()) {
                        Text(details, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = TextAlign.Center)
                    }
                    if (overview.isNotBlank()) {
                        Text(
                            overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = .9f),
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Text(
                        "Press OK to continue",
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.Muted,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
        }

        // ---- brief seek hint while controls are hidden
        hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .background(Color(0xB0000000), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // ---- skip intro (bottom right; sits above the glass panel while controls are shown)
        AnimatedVisibility(
            visible = showSkipIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = if (showControls) 140.dp else 48.dp),
        ) {
            TvButton("Skip Intro", { skipIntro() }, selected = true)
        }

        // ---- controls: top bar + glass panel
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopStart)
                        .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))),
                )

                Row(
                    Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 24.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    GlassIconButton(onClick = onExit) { tint -> Glyph(GlyphKind.Back, tint, Modifier.size(22.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).padding(top = 2.dp)) {
                        Text(
                            title.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (topMeta.isNotEmpty()) {
                            Text(
                                topMeta.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Palette.Silver,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    GlassIconButton(onClick = { panel = Panel.Subtitles }) { tint ->
                        Glyph(GlyphKind.Subtitles, tint, Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(18.dp))
                    AlveonWordmark(Modifier.padding(top = 2.dp), logoSize = 32.dp)
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 26.dp)
                        .glass(RoundedCornerShape(26.dp))
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(positionMs), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(62.dp))
                        SeekBar(
                            positionMs, durationMs,
                            onSeekBy = { seekBy(it); interactionTick++ },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatTime(durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(72.dp),
                        )
                    }
                    Box(Modifier.fillMaxWidth()) {
                        val left = if (isSeries) "Season ${parsed.season ?: 1}  ·  Episode ${parsed.episode ?: 1}"
                        else facts.joinToString("  ·  ")
                        Text(
                            left,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterStart).widthIn(max = 230.dp),
                        )
                        Row(
                            Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            GlassIconButton(onClick = { goPrevious(); interactionTick++ }, enabled = prevEntry != null || positionMs > 5_000) { tint ->
                                Glyph(GlyphKind.Prev, tint, Modifier.size(20.dp))
                            }
                            GlassIconButton(onClick = { seekBy(-SEEK_STEP_MS); interactionTick++ }) { tint ->
                                SkipGlyph(forward = false, tint = tint, iconSize = 26.dp)
                            }
                            FocusSurface(
                                onClick = { togglePlay(); interactionTick++ },
                                modifier = Modifier.size(48.dp).focusRequester(playFocus),
                                shape = CircleShape,
                                container = Palette.Silver,
                                focusedContainer = Color.White,
                                focusedScale = 1.1f,
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Glyph(
                                        if (playWhenReady) GlyphKind.Pause else GlyphKind.Play,
                                        Palette.Black,
                                        Modifier.size(22.dp),
                                    )
                                }
                            }
                            GlassIconButton(onClick = { seekBy(SEEK_STEP_MS); interactionTick++ }) { tint ->
                                SkipGlyph(forward = true, tint = tint, iconSize = 26.dp)
                            }
                            GlassIconButton(
                                onClick = { nextEntry?.let { markFinished(); playOther.value(it) } },
                                enabled = nextEntry != null,
                            ) { tint -> Glyph(GlyphKind.Next, tint, Modifier.size(20.dp)) }
                        }
                        Row(
                            Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GlassIconButton(onClick = { panel = Panel.Audio }) { tint ->
                                Glyph(GlyphKind.Speaker, tint, Modifier.size(22.dp))
                            }
                            GlassIconButton(onClick = { panel = Panel.Speed }) { tint ->
                                Glyph(GlyphKind.Gear, tint, Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }

        panel?.let { p ->
            val options = remember(p, tracks, trackTick) {
                when (p) {
                    Panel.Audio -> audioOptions(player, tracks)
                    Panel.Subtitles -> subtitleOptions(player, tracks)
                    Panel.Speed -> speedOptions(player)
                }
            }
            TrackPanel(
                title = when (p) {
                    Panel.Audio -> "Audio"
                    Panel.Subtitles -> "Subtitles"
                    Panel.Speed -> "Playback speed"
                },
                options = options,
                emptyText = if (p == Panel.Audio) "No alternative audio tracks." else "No subtitles found (embedded or external .srt/.vtt).",
                onClose = { panel = null },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        error?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).widthIn(max = 560.dp).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Can't play this video", style = MaterialTheme.typography.headlineMedium)
                Text(msg, color = Palette.Muted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton("Back", onExit)
                    TvButton("Retry", {
                        error = null
                        player.prepare()
                    }, selected = true)
                }
            }
        }
    }
}

@Composable
private fun TrackPanel(
    title: String,
    options: List<TrackOption>,
    emptyText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember(options.size) { List(options.size.coerceAtLeast(1)) { FocusRequester() } }
    val initial = options.indexOfFirst { it.selected }.coerceAtLeast(0)
    LaunchedEffect(options.size) {
        delay(80)
        runCatching { focusRequesters[initial].requestFocus() }
    }
    Column(
        modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Color(0xF2080809))
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (options.isEmpty()) {
            Text(emptyText, color = Palette.Muted, style = MaterialTheme.typography.bodyLarge)
            TvButton("Close", onClose, Modifier.focusRequester(focusRequesters[0]))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(options) { index, option ->
                    FocusSurface(
                        onClick = { option.onSelect(); onClose() },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[index]),
                        shape = RoundedCornerShape(12.dp),
                        focusedScale = 1.03f,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (option.selected) Palette.Silver else Color.Transparent),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(option.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeekBy: (Long) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val barHeight = if (focused) 7.dp else 4.dp
    Box(
        modifier
            .height(26.dp)
            .onFocusChanged { focused = it.hasFocus }
            .onKeyEvent { ev ->
                val isSide = ev.key == Key.DirectionLeft || ev.key == Key.DirectionRight
                if (isSide && ev.type == KeyEventType.KeyDown) {
                    onSeekBy(if (ev.key == Key.DirectionLeft) -SEEK_STEP_MS else SEEK_STEP_MS)
                }
                isSide
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(barHeight).clip(CircleShape).background(Color.White.copy(alpha = .28f)))
        Box(Modifier.fillMaxWidth(fraction).height(barHeight).clip(CircleShape).background(Color.White))
        if (focused) {
            Box(
                Modifier.padding(start = 0.dp).fillMaxWidth(fraction).height(barHeight),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
