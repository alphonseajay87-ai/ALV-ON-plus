package com.alveon.plus.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

const val TMDB_API_KEY = "ec21e330efd2aeaa592e023e9cd18c64"
const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
const val TMDB_POSTER_BASE = "https://image.tmdb.org/t/p/w342"
const val TMDB_BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"

data class TmdbSearchResponse(val results: List<TmdbResult> = emptyList())

data class TmdbResult(
    val id: Int? = null,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
)

data class TmdbEpisode(val name: String? = null, val overview: String? = null)

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") key: String,
        @Query("query") query: String,
        @Query("year") year: String? = null,
        @Query("language") language: String? = null,
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") key: String,
        @Query("query") query: String,
        @Query("first_air_date_year") year: String? = null,
        @Query("language") language: String? = null,
    ): TmdbSearchResponse

    @GET("tv/{id}/season/{season}/episode/{episode}")
    suspend fun episode(
        @Path("id") id: Int,
        @Path("season") season: Int,
        @Path("episode") episode: Int,
        @Query("api_key") key: String,
        @Query("language") language: String? = null,
    ): TmdbEpisode
}
