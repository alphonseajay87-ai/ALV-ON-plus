package com.alveon.plus.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Every video found in the saved folders and servers, with posters filled in as they arrive. */
class LibraryRepository(
    private val store: LocalStore,
    private val tmdb: TmdbRepository,
    private val servers: ServerBrowser,
) {
    private val _items = MutableStateFlow<List<LibraryItem>>(emptyList())
    val items: StateFlow<List<LibraryItem>> = _items.asStateFlow()

    suspend fun refresh(context: Context) {
        val local = store.locations.value.flatMap { loc ->
            runCatching { StorageBrowser.scanVideos(context, Uri.parse(loc.treeUri)) }.getOrDefault(emptyList())
        }
        val remote = store.servers.value.flatMap { s ->
            runCatching { servers.scanVideos(s) }.getOrDefault(emptyList())
        }
        val found = (local + remote).distinctBy { it.uri }.sortedBy { it.name.lowercase() }.take(400)
        _items.value = found.map { LibraryItem(it, tmdb.cached(it.name)) }

        val gate = Semaphore(3)
        coroutineScope {
            _items.value.filter { it.info == null }.forEach { item ->
                launch {
                    gate.withPermit {
                        val info = tmdb.lookup(item.entry.name)
                        if (info != null) {
                            _items.update { list ->
                                list.map { if (it.entry.uri == item.entry.uri) it.copy(info = info) else it }
                            }
                        }
                    }
                }
            }
        }
    }

    /** The list "next / previous" should walk through when [entry] is played. */
    fun queueFor(entry: MediaEntry): List<MediaEntry> {
        val p = FileNameCleaner.parse(entry.name)
        if (!p.isEpisode) return listOf(entry)
        return _items.value
            .filter { it.parsed.isEpisode && it.parsed.title.equals(p.title, ignoreCase = true) }
            .sortedWith(compareBy({ it.parsed.season ?: 0 }, { it.parsed.episode ?: 0 }))
            .map { it.entry }
            .ifEmpty { listOf(entry) }
    }
}
