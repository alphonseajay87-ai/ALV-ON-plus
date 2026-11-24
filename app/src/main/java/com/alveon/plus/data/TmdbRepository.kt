package com.alveon.plus.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

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
    suspend fun searchMovie(@Query("api_key") key: String, @Query("query") query: String): MovieSearchResponse
}

object TmdbRepository {
    private const val API_KEY = "ec21e330efd2aeaa592e023e9cd18c64"
    private val api = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(TmdbApi::class.java)

    suspend fun lookup(filename: String): MovieInfo {
        val clean = filename.substringBeforeLast('.', filename)
            .replace(Regex("[._]"), " ")
            .replace(Regex("\\b(19|20)\\d{2}\\b"), " ")
            .replace(Regex("\\b(1080p|720p|2160p|4k|bluray|webrip|web-dl|hdtv)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ").trim()
        return try {
            val result = api.searchMovie(API_KEY, clean).results.firstOrNull()
                ?: return MovieInfo(clean.ifBlank { filename })
            MovieInfo(
                title = result.title ?: clean,
                overview = result.overview.orEmpty(),
                year = result.release_date?.take(4).orEmpty(),
                rating = result.vote_average?.let { "%.1f".format(it) }.orEmpty(),
                posterUrl = result.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
            )
        } catch (_: Exception) { MovieInfo(clean.ifBlank { filename }) }
    }
}
