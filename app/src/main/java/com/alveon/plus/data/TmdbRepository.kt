package com.alveon.plus.data

import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.util.Locale

/**
 * Looks up movie / series / episode info from a file name. Results are cached on disk so the
 * app keeps showing posters and descriptions when offline.
 */
class TmdbRepository(private val api: TmdbApi, private val store: LocalStore) {

    private val memory = LinkedHashMap<String, MovieInfo>()
    private val misses = HashSet<String>()
    private var loaded = false

    private fun baseKey(p: ParsedName) =
        "${if (p.isEpisode) "tv" else "mv"}|${p.title.lowercase()}|${p.year.orEmpty()}"

    private fun key(p: ParsedName) =
        if (p.isEpisode) "${baseKey(p)}|${p.season}|${p.episode}" else baseKey(p)

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val obj = JSONObject(store.readTmdbCache())
            obj.keys().forEach { k -> memory[k] = obj.getJSONObject(k).toInfo() }
        }
    }

    @Synchronized
    fun cached(fileName: String): MovieInfo? {
        ensureLoaded()
        return memory[key(FileNameCleaner.parse(fileName))]
    }

    suspend fun lookup(fileName: String): MovieInfo? {
        val parsed = FileNameCleaner.parse(fileName)
        if (parsed.title.length < 2) return null
        val k = key(parsed)
        synchronized(this) {
            ensureLoaded()
            memory[k]?.let { return it }
            if (k in misses) return null
        }
        if (!store.tmdbEnabled.value) return null

        return try {
            val lang = Locale.getDefault().toLanguageTag()
            val base = findBase(parsed, lang)
            if (base == null) {
                synchronized(this) { misses += k }
                return null
            }
            val season = parsed.season
            val episode = parsed.episode
            val full = if (parsed.isEpisode && season != null && episode != null && base.tmdbId != 0) {
                withEpisode(base, season, episode, lang)
            } else base
            remember(k, full)
            full
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // offline or API error: fall back to the cleaned file name
        }
    }

    private suspend fun findBase(p: ParsedName, lang: String): MovieInfo? {
        val bk = baseKey(p)
        synchronized(this) { memory[bk]?.let { return it } }

        var results = search(p, p.year, lang)
        if (results.isEmpty() && p.year != null) results = search(p, null, lang)
        val hit = results.firstOrNull { it.posterPath != null } ?: results.firstOrNull() ?: return null

        var overview = hit.overview.orEmpty()
        if (overview.isBlank() && !lang.startsWith("en")) {
            // Not translated yet: fall back to the English description.
            val en = runCatching { search(p, p.year, "en-US") }.getOrDefault(emptyList())
                .firstOrNull { it.id == hit.id }
            overview = en?.overview.orEmpty()
        }
        val info = MovieInfo(
            title = hit.title ?: hit.name ?: p.title,
            overview = overview,
            year = (hit.releaseDate ?: hit.firstAirDate).orEmpty().take(4),
            rating = hit.voteAverage?.takeIf { it > 0 }?.let { "%.1f".format(Locale.US, it) }.orEmpty(),
            posterUrl = hit.posterPath?.let { TMDB_POSTER_BASE + it },
            backdropUrl = hit.backdropPath?.let { TMDB_BACKDROP_BASE + it },
            tmdbId = hit.id ?: 0,
            isSeries = p.isEpisode,
        )
        remember(bk, info)
        return info
    }

    private suspend fun withEpisode(base: MovieInfo, season: Int, episode: Int, lang: String): MovieInfo {
        return try {
            var e = api.episode(base.tmdbId, season, episode, TMDB_API_KEY, lang)
            if (e.overview.isNullOrBlank() && !lang.startsWith("en")) {
                e = runCatching { api.episode(base.tmdbId, season, episode, TMDB_API_KEY, "en-US") }
                    .getOrDefault(e)
            }
            base.copy(episodeTitle = e.name.orEmpty(), episodeOverview = e.overview.orEmpty())
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            base
        }
    }

    private suspend fun search(p: ParsedName, year: String?, lang: String): List<TmdbResult> =
        if (p.isEpisode) api.searchTv(TMDB_API_KEY, p.title, year, lang).results
        else api.searchMovie(TMDB_API_KEY, p.title, year, lang).results

    @Synchronized
    private fun remember(k: String, info: MovieInfo) {
        memory[k] = info
        while (memory.size > 800) memory.remove(memory.keys.first())
        val obj = JSONObject()
        memory.forEach { (key, v) -> obj.put(key, v.toJson()) }
        store.writeTmdbCache(obj.toString())
    }

    @Synchronized
    fun clear() {
        memory.clear()
        misses.clear()
        loaded = true
        store.writeTmdbCache("{}")
    }
}

private fun MovieInfo.toJson() = JSONObject()
    .put("title", title).put("overview", overview).put("year", year).put("rating", rating)
    .put("poster", posterUrl.orEmpty()).put("backdrop", backdropUrl.orEmpty())
    .put("id", tmdbId).put("series", isSeries)
    .put("epTitle", episodeTitle).put("epOverview", episodeOverview)

private fun JSONObject.toInfo() = MovieInfo(
    title = getString("title"),
    overview = optString("overview"),
    year = optString("year"),
    rating = optString("rating"),
    posterUrl = optString("poster").ifBlank { null },
    backdropUrl = optString("backdrop").ifBlank { null },
    tmdbId = optInt("id"),
    isSeries = optBoolean("series"),
    episodeTitle = optString("epTitle"),
    episodeOverview = optString("epOverview"),
)
