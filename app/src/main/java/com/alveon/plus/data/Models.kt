package com.alveon.plus.data

enum class ServerType { HTTP, WEBDAV }

data class MediaEntry(
    val name: String,
    val uri: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = -1L,
    /** Epoch millis when the file was last modified, or -1 if unknown. */
    val lastModified: Long = -1L,
)

data class ServerEntry(
    val id: String,
    val name: String,
    val url: String,
    val type: ServerType = ServerType.HTTP,
    val username: String = "",
    val password: String = "",
)

data class StorageLocation(val name: String, val treeUri: String)

data class MovieInfo(
    val title: String,
    val overview: String = "",
    val year: String = "",
    val rating: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val tmdbId: Int = 0,
    val isSeries: Boolean = false,
    val episodeTitle: String = "",
    val episodeOverview: String = "",
)

/** A video found in a saved folder or server, with its (cached) TMDB info. */
data class LibraryItem(val entry: MediaEntry, val info: MovieInfo?) {
    val parsed: ParsedName by lazy { FileNameCleaner.parse(entry.name) }
}

data class WatchProgress(
    val uri: String,
    val fileName: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
