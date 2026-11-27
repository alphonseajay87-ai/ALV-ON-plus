package com.alveon.plus.ui.home

import com.alveon.plus.AppGraph
import com.alveon.plus.data.LibraryItem
import com.alveon.plus.data.MediaEntry
import com.alveon.plus.data.MovieInfo
import com.alveon.plus.data.WatchProgress

/** One tile on the home screen: a movie, a whole series, or a half-watched video. */
data class HomeItem(
    val key: String,
    val title: String,
    val info: MovieInfo?,
    val isSeries: Boolean,
    val entry: MediaEntry?,
    val episodes: List<LibraryItem> = emptyList(),
    val progress: Float? = null,
) {
    val isContinue: Boolean get() = key.startsWith("cw:")
    val favKey: String get() = key.removePrefix("cw:")
}

fun seriesKey(title: String) = "series:" + title.lowercase()

/** Splits the library into movies and (grouped) series, both sorted by title. */
fun groupLibrary(items: List<LibraryItem>): Pair<List<HomeItem>, List<HomeItem>> {
    val movies = items.filter { !it.parsed.isEpisode }.map { item ->
        HomeItem(
            key = item.entry.uri,
            title = item.info?.title ?: item.parsed.title,
            info = item.info,
            isSeries = false,
            entry = item.entry,
        )
    }.sortedBy { it.title.lowercase() }

    val series = items.filter { it.parsed.isEpisode }
        .groupBy { it.parsed.title.lowercase() }
        .map { (_, eps) ->
            val sorted = eps.sortedWith(compareBy({ it.parsed.season ?: 0 }, { it.parsed.episode ?: 0 }))
            val info = sorted.firstNotNullOfOrNull { it.info }
            val name = info?.title ?: sorted.first().parsed.title
            HomeItem(
                key = seriesKey(sorted.first().parsed.title),
                title = name,
                info = info,
                isSeries = true,
                entry = null,
                episodes = sorted,
            )
        }.sortedBy { it.title.lowercase() }
    return movies to series
}

fun WatchProgress.toHomeItem(): HomeItem {
    val info = AppGraph.tmdb.cached(fileName)
        ?: MovieInfo(title = title, posterUrl = posterUrl, backdropUrl = backdropUrl)
    return HomeItem(
        key = "cw:$uri",
        title = info.title.ifBlank { title },
        info = info,
        isSeries = false,
        entry = MediaEntry(fileName, uri, false),
        progress = fraction,
    )
}

/**
 * The episode "Play" should start: the one in progress, otherwise the one after the last
 * finished episode, otherwise the first.
 */
fun nextEpisodeFor(item: HomeItem): LibraryItem? {
    val eps = item.episodes
    if (eps.isEmpty()) return null
    val store = AppGraph.store
    val inProgress = store.progress.value
        .filter { p -> eps.any { it.entry.uri == p.uri } }
        .maxByOrNull { it.updatedAt }
    if (inProgress != null) return eps.first { it.entry.uri == inProgress.uri }
    val doneUri = store.lastFinished(item.key)
    val doneIndex = eps.indexOfFirst { it.entry.uri == doneUri }
    if (doneIndex >= 0) return eps.getOrNull(doneIndex + 1) ?: eps.first()
    return eps.first()
}

/** Plays a movie, or a series' next episode. [onSeries] is kept for callers that open the list. */
@Suppress("UNUSED_PARAMETER")
fun openHomeItem(item: HomeItem, onPlay: (MediaEntry) -> Unit, onSeries: (HomeItem) -> Unit) {
    if (item.isSeries) {
        nextEpisodeFor(item)?.let {
            AppGraph.queue = AppGraph.library.queueFor(it.entry)
            onPlay(it.entry)
        }
    } else {
        item.entry?.let {
            AppGraph.queue = AppGraph.library.queueFor(it)
            onPlay(it)
        }
    }
}
