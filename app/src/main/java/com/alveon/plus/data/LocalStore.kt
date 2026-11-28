package com.alveon.plus.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Servers, saved folders, continue-watching and settings, kept in SharedPreferences. */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("alveon_store", Context.MODE_PRIVATE)

    private val _servers = MutableStateFlow(readServers())
    val servers: StateFlow<List<ServerEntry>> = _servers.asStateFlow()

    private val _locations = MutableStateFlow(readLocations())
    val locations: StateFlow<List<StorageLocation>> = _locations.asStateFlow()

    private val _progress = MutableStateFlow(readProgress())
    val progress: StateFlow<List<WatchProgress>> = _progress.asStateFlow()

    private val _tmdbEnabled = MutableStateFlow(prefs.getBoolean("tmdb_enabled", true))
    val tmdbEnabled: StateFlow<Boolean> = _tmdbEnabled.asStateFlow()

    /** BCP-47 language tag preferred for audio (e.g. "en", "es"). Empty = best quality track. */
    private val _preferredAudioLang = MutableStateFlow(prefs.getString("pref_audio_lang", "") ?: "")
    val preferredAudioLang: StateFlow<String> = _preferredAudioLang.asStateFlow()

    /** BCP-47 language tag preferred for subtitles. Empty = Off / first available. */
    private val _preferredSubLang = MutableStateFlow(prefs.getString("pref_sub_lang", "") ?: "")
    val preferredSubLang: StateFlow<String> = _preferredSubLang.asStateFlow()

    fun setPreferredAudioLang(tag: String) {
        _preferredAudioLang.value = tag
        prefs.edit().putString("pref_audio_lang", tag).apply()
    }

    fun setPreferredSubLang(tag: String) {
        _preferredSubLang.value = tag
        prefs.edit().putString("pref_sub_lang", tag).apply()
    }

    // ---- servers
    fun serverById(id: String) = _servers.value.firstOrNull { it.id == id }

    fun addServer(server: ServerEntry) {
        _servers.value = _servers.value.filterNot { it.id == server.id } + server
        writeServers()
    }

    fun removeServer(id: String) {
        _servers.value = _servers.value.filterNot { it.id == id }
        writeServers()
    }

    private fun writeServers() = writeArray("servers", _servers.value.map {
        JSONObject().put("id", it.id).put("name", it.name).put("url", it.url)
            .put("type", it.type.name).put("username", it.username).put("password", it.password)
    })

    // ---- storage locations
    fun addLocation(location: StorageLocation) {
        _locations.value = _locations.value.filterNot { it.treeUri == location.treeUri } + location
        writeLocations()
    }

    fun removeLocation(treeUri: String) {
        _locations.value = _locations.value.filterNot { it.treeUri == treeUri }
        writeLocations()
    }

    private fun writeLocations() = writeArray("locations", _locations.value.map {
        JSONObject().put("name", it.name).put("treeUri", it.treeUri)
    })

    // ---- last finished episode per series
    fun lastFinished(seriesKey: String): String? = prefs.getString("done_$seriesKey", null)

    fun setLastFinished(seriesKey: String, uri: String) {
        prefs.edit().putString("done_$seriesKey", uri).apply()
    }

    // ---- continue watching
    fun progressFor(uri: String) = _progress.value.firstOrNull { it.uri == uri }

    fun saveProgress(p: WatchProgress) {
        if (p.durationMs > 0 && p.positionMs > p.durationMs * 0.95) {
            removeProgress(p.uri)
            return
        }
        if (p.positionMs < 15_000) return
        _progress.value = (listOf(p) + _progress.value.filterNot { it.uri == p.uri }).take(20)
        writeProgress()
    }

    fun removeProgress(uri: String) {
        _progress.value = _progress.value.filterNot { it.uri == uri }
        writeProgress()
    }

    private fun writeProgress() = writeArray("progress", _progress.value.map {
        JSONObject().put("uri", it.uri).put("fileName", it.fileName).put("title", it.title)
            .put("poster", it.posterUrl.orEmpty()).put("backdrop", it.backdropUrl.orEmpty())
            .put("pos", it.positionMs).put("dur", it.durationMs).put("at", it.updatedAt)
    })

    // ---- my list + onboarding
    private val _favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun toggleFavorite(key: String) {
        val next = if (key in _favorites.value) _favorites.value - key else _favorites.value + key
        _favorites.value = next
        prefs.edit().putStringSet("favorites", HashSet(next)).apply()
    }

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit().putBoolean("onboarded", value).apply()

    // ---- settings / cache
    fun setTmdbEnabled(enabled: Boolean) {
        _tmdbEnabled.value = enabled
        prefs.edit().putBoolean("tmdb_enabled", enabled).apply()
    }

    fun readTmdbCache(): String = prefs.getString("tmdb_cache2", "{}") ?: "{}"
    fun writeTmdbCache(json: String) = prefs.edit().putString("tmdb_cache2", json).apply()

    // ---- json helpers
    private fun readServers() = readArray("servers") { o ->
        ServerEntry(
            id = o.getString("id"),
            name = o.getString("name"),
            url = o.getString("url"),
            type = runCatching { ServerType.valueOf(o.optString("type", "HTTP")) }.getOrDefault(ServerType.HTTP),
            username = o.optString("username"),
            password = o.optString("password"),
        )
    }

    private fun readLocations() = readArray("locations") { o ->
        StorageLocation(o.getString("name"), o.getString("treeUri"))
    }

    private fun readProgress() = readArray("progress") { o ->
        WatchProgress(
            uri = o.getString("uri"),
            fileName = o.getString("fileName"),
            title = o.getString("title"),
            posterUrl = o.optString("poster").ifBlank { null },
            backdropUrl = o.optString("backdrop").ifBlank { null },
            positionMs = o.getLong("pos"),
            durationMs = o.getLong("dur"),
            updatedAt = o.getLong("at"),
        )
    }.sortedByDescending { it.updatedAt }

    private fun <T> readArray(key: String, map: (JSONObject) -> T): List<T> = runCatching {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).mapNotNull { i -> runCatching { map(arr.getJSONObject(i)) }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun writeArray(key: String, items: List<JSONObject>) {
        prefs.edit().putString(key, JSONArray(items).toString()).apply()
    }
}
