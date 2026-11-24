package com.alveon.plus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.foundation.lazy.TvLazyRow
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.alveon.plus.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Black = Color(0xFF000000)
private val Silver = Color(0xFFD8D8D8)
private val Muted = Color(0xFF999999)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { AlveonApp() }
    }
}

@Composable
private fun AlveonApp() {
    val context = LocalContext.current
    val repo = remember { LocalRepository(context) }
    var showSplash by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<MediaEntry?>(null) }
    var movie by remember { mutableStateOf<MovieInfo?>(null) }
    var entries by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var currentServer by remember { mutableStateOf<ServerEntry?>(null) }
    var servers by remember { mutableStateOf(repo.servers()) }
    val scope = rememberCoroutineScope()
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch { entries = browseTree(context, uri); page = "browser" }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Black,
            surface = Color(0xFF111111),
            primary = Silver,
            onPrimary = Black,
            onBackground = Silver,
            onSurface = Silver
        )
    ) {
        Box(Modifier.fillMaxSize().background(Black)) {
            if (showSplash) Splash { showSplash = false }
            else when (page) {
                "home" -> HomeScreen(
                    repo,
                    onBrowse = { treePicker.launch(null) },
                    onAddServer = { page = "addServer" },
                    onSettings = { page = "settings" },
                    onPlay = { entry ->
                        selected = entry
                        page = "loading"
                        scope.launch {
                            movie = TmdbRepository.lookup(entry.name)
                            repo.addRecent(entry)
                            page = "player"
                        }
                    },
                    onServer = { server ->
                        currentServer = server
                        page = "server"
                        entries = emptyList()
                        scope.launch {
                            runCatching { fetchServerEntries(server.url) }
                                .onSuccess { entries = it }
                                .onFailure { entries = emptyList() }
                        }
                    }
                )
                "browser" -> EntryList("USB / Storage", entries, { page = "home" }) { entry ->
                    if (entry.isDirectory) {
                        val uri = Uri.parse(entry.uri)
                        scope.launch { entries = browseTree(context, uri) }
                    } else {
                        selected = entry
                        page = "loading"
                        scope.launch {
                            movie = TmdbRepository.lookup(entry.name)
                            repo.addRecent(entry)
                            page = "player"
                        }
                    }
                }
                "server" -> EntryList(currentServer?.name ?: "Server", entries, { page = "home" }) { entry ->
                    if (entry.isDirectory) {
                        scope.launch {
                            runCatching { fetchServerEntries(entry.uri) }.onSuccess { entries = it }
                        }
                    } else {
                        selected = entry
                        page = "loading"
                        scope.launch {
                            movie = TmdbRepository.lookup(entry.name)
                            repo.addRecent(entry)
                            page = "player"
                        }
                    }
                }
                "addServer" -> AddServerScreen(
                    onBack = { page = "home" },
                    onSave = { server ->
                        repo.saveServer(server)
                        servers = repo.servers()
                        page = "home"
                    }
                )
                "settings" -> SettingsScreen(
                    servers,
                    onBack = { page = "home" },
                    onClear = { repo.clearCache(context) },
                    onRemove = { server ->
                        repo.removeServer(server)
                        servers = repo.servers()
                    },
                    onAdd = { page = "addServer" }
                )
                "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Silver)
                }
                "player" -> selected?.let { PlayerScreen(it, movie, { page = "home" }) }
            }
        }
    }
}

@Composable
private fun Splash(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else .72f,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "logo-scale"
    )
    LaunchedEffect(Unit) {
        visible = true
        delay(1_100)
        onDone()
    }
    Column(
        Modifier.fillMaxSize().background(Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "ALVéON+ logo",
                    modifier = Modifier.size(82.dp).scale(scale)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "ALVéON+",
                    color = Silver,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    repo: LocalRepository,
    onBrowse: () -> Unit,
    onAddServer: () -> Unit,
    onSettings: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onServer: (ServerEntry) -> Unit
) {
    val recents = remember { repo.recents() }
    val servers = remember { repo.servers() }
    Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 44.dp, top = 28.dp, bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            Text("ALVéON+", color = Silver, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            NavButton("From USB / Storage", onBrowse)
            NavButton("Add server", onAddServer)
            NavButton("Settings", onSettings)
        }
        Spacer(Modifier.height(38.dp))
        if (recents.isNotEmpty()) MediaRow("Continue Watching", recents, onPlay)
        MediaRow("From USB / Storage", emptyList(), onBrowse, "Browse a USB drive or choose a folder to get started")
        Text("From Server", color = Silver, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        if (servers.isNotEmpty()) {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(servers.size) { index ->
                    val server = servers[index]
                    PosterCard(server.name, server.url, null) { onServer(server) }
                }
            }
        } else {
            Text("Add an HTTP directory or direct video URL to browse your library.", color = Muted, fontSize = 15.sp)
        }
    }
}

@Composable
private fun MediaRow(
    title: String,
    items: List<MediaEntry>,
    onClick: (MediaEntry) -> Unit,
    emptyMessage: String? = null
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        Text(title, color = Silver, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            NavButton(emptyMessage ?: "Open", { })
        } else {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(items.size) { index ->
                    val item = items[index]
                    PosterCard(item.name.substringBeforeLast('.'), item.name, null) { onClick(item) }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(title: String, subtitle: String, image: String?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(172.dp)
            .onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.04f else 1f)
            .border(if (focused) 2.dp else 1.dp, if (focused) Silver else Color(0xFF333333), RoundedCornerShape(8.dp))
            .background(Color(0xFF151515), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .focusable()
    ) {
        Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
            if (image != null) AsyncImage(model = image, contentDescription = title, modifier = Modifier.fillMaxSize())
            else Text("▶", color = Silver.copy(alpha = .8f), fontSize = 28.sp)
        }
        Text(title, color = Silver, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
private fun NavButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text,
        color = if (focused) Black else Silver,
        fontSize = 14.sp,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) Silver else Color.Transparent, RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun EntryList(title: String, entries: List<MediaEntry>, onBack: () -> Unit, onSelect: (MediaEntry) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavButton("‹  Back", onBack)
            Spacer(Modifier.width(24.dp))
            Text(title, color = Silver, fontSize = 26.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(24.dp))
        if (entries.isEmpty()) Text("No folders or videos found here.", color = Muted, fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                var focused by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .background(if (focused) Color(0xFF252525) else Color(0xFF101010), RoundedCornerShape(8.dp))
                        .clickable { onSelect(entry) }
                        .focusable()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (entry.isDirectory) "▰" else "▶", color = Silver, fontSize = 18.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(entry.name, color = Silver, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun AddServerScreen(onBack: () -> Unit, onSave: (ServerEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(56.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        NavButton("‹  Back", onBack)
        Text("Add a server", color = Silver, fontSize = 28.sp)
        InputField("Server name", name) { name = it }
        InputField("URL (http:// or https://)", url) { url = it }
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF7777))
        NavButton("Save server") {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                onSave(ServerEntry(name.ifBlank { "My Server" }, url.trim()))
            } else error = "Enter a valid HTTP or HTTPS URL."
        }
        Text("HTTP directory listings and direct video links are supported.", color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun InputField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = Muted, fontSize = 14.sp)
        BasicTextField(
            value,
            onChange,
            singleLine = true,
            textStyle = TextStyle(color = Silver, fontSize = 18.sp),
            modifier = Modifier.width(600.dp)
                .background(Color(0xFF171717), RoundedCornerShape(6.dp))
                .focusable()
                .padding(14.dp)
        )
    }
}

@Composable
private fun SettingsScreen(
    servers: List<ServerEntry>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onRemove: (ServerEntry) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(56.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NavButton("‹  Back", onBack)
        Text("Settings", color = Silver, fontSize = 28.sp)
        Text("Manage servers", color = Silver, fontSize = 18.sp)
        servers.forEach { server ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${server.name}  ·  ${server.url}", color = Muted, modifier = Modifier.width(600.dp))
                NavButton("Remove") { onRemove(server) }
            }
        }
        NavButton("Add server", onAdd)
        NavButton("Clear cache", onClear)
        Text("ALVéON+  ·  Free, offline-first media player", color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun PlayerScreen(entry: MediaEntry, info: MovieInfo?, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(entry.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(entry.uri)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Black)) {
        AndroidView(
            factory = { PlayerView(it).apply {
                this.player = player
                useController = true
                controllerShowTimeoutMs = 5_000
            } },
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.align(Alignment.TopStart).padding(36.dp).widthIn(max = 720.dp)) {
            Text(info?.title ?: entry.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            val details = listOfNotNull(
                info?.year?.takeIf { it.isNotBlank() },
                info?.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" }
            ).joinToString("  ·  ")
            if (details.isNotBlank()) Text(details, color = Silver, fontSize = 14.sp)
            if (!info?.overview.isNullOrBlank()) {
                Text(info!!.overview, color = Color.White.copy(alpha = .85f), fontSize = 14.sp, maxLines = 3, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(30.dp)) {
            NavButton("‹  Back", onBack)
        }
    }
}
