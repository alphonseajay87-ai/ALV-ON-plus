package com.alveon.plus

import android.content.Context
import coil.imageLoader
import com.alveon.plus.data.LibraryRepository
import com.alveon.plus.data.LocalStore
import com.alveon.plus.data.MediaEntry
import com.alveon.plus.data.ServerAuthInterceptor
import com.alveon.plus.data.ServerBrowser
import com.alveon.plus.data.TMDB_BASE_URL
import com.alveon.plus.data.TmdbApi
import com.alveon.plus.data.TmdbRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Tiny service locator; everything here lives for the whole process. */
object AppGraph {
    lateinit var appContext: Context
        private set
    lateinit var store: LocalStore
        private set
    lateinit var httpClient: OkHttpClient
        private set
    lateinit var tmdb: TmdbRepository
        private set
    lateinit var serverBrowser: ServerBrowser
        private set
    lateinit var library: LibraryRepository
        private set

    /** What the player's next / previous buttons walk through. Set right before opening the player. */
    var queue: List<MediaEntry> = emptyList()

    fun init(context: Context) {
        appContext = context.applicationContext
        store = LocalStore(appContext)
        httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(ServerAuthInterceptor { store.servers.value })
            .build()
        val api = Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
        tmdb = TmdbRepository(api, store)
        serverBrowser = ServerBrowser(httpClient)
        library = LibraryRepository(store, tmdb, serverBrowser)
    }

    /** Clears posters, cached movie info and temp files. Call off the main thread. */
    fun clearCaches() {
        appContext.imageLoader.memoryCache?.clear()
        appContext.imageLoader.diskCache?.clear()
        tmdb.clear()
        appContext.cacheDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
    }
}
