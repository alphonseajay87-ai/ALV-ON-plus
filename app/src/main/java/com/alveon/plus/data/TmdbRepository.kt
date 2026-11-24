package com.alveon.plus.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val TMDB_API_KEY = "ec21e330efd2aeaa592e023e9cd18c64"

data class MovieSearchResponse(val results: List<MovieDto> = emptyList())
data class MovieDto(
    val title: String? = null,
    val overview: String? = null,
    val release_date: String? = null,
    val vote_average: Double? = null,
    val poster_path: String? = null
)

data class MovieInfo(
    val title: String,
    val overview: String = "",
    val year: String = "",
    val rating: String = "",
    val posterUrl: String? = null
)

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") key: String,
        @Query("query") query: String
    ): MovieSearchResponse
}

object TmdbRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    suspend fun lookup(filename: String): MovieInfo {
        val extensionRemoved = filename.substringBeforeLast('.', filename)
        val clean = extensionRemoved
            .replace(Regex("[._]"), " ")
            .replace(Regex("\\b(?:19|20)\\d{2}\b"), " ")
            .replace(
                Regex("\\b(?:1080p|720p|2160p|4k|bluray|brrip|webrip|web-dl|hdtv)\\b", RegexOption.IGNORE_CASE),
                " "
            )
            .replace(Regex("\\b(?:x264|x265|h264|h265|hevc|aac|ac3|dts)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (clean.isBlank()) return MovieInfo(filename)

        return try {
            val match = api.searchMovie(TMDB_API_KEY, clean).results.firstOrNull()
                ?: return MovieInfo(clean)
            MovieInfo(
                title = match.title ?: clean,
                overview = match.overview.orEmpty(),
                year = match.release_date?.take(4).orEmpty(),
                rating = match.vote_average?.let { "%.1f".format(it) }.orEmpty(),
                posterUrl = match.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
            )
        } catch (_: Exception) {
            MovieInfo(clean)
        }
    }
}
