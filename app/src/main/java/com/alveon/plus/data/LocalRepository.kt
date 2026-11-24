package com.alveon.plus.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

private const val PREFS = "alveon_local"
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "ts", "mpeg", "mpg", "3gp")

data class MediaEntry(val name: String, val uri: String, val isDirectory: Boolean)
data class ServerEntry(val name: String, val url: String)

class LocalRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun servers(): List<ServerEntry> = runCatching {
        val data = JSONArray(prefs.getString("servers", "[]") ?: "[]")
        (0 until data.length()).map { index ->
            val obj = data.getJSONObject(index)
            ServerEntry(obj.getString("name"), obj.getString("url"))
        }
    }.getOrDefault(emptyList())

    fun saveServer(server: ServerEntry) {
        saveServers(servers().filterNot { it.url == server.url } + server)
    }

    fun removeServer(server: ServerEntry) {
        saveServers(servers().filterNot { it == server })
    }

    private fun saveServers(items: List<ServerEntry>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().put("name", item.name).put("url", item.url))
        }
        prefs.edit().putString("servers", arr.toString()).apply()
    }

    fun recents(): List<MediaEntry> = runCatching {
        val arr = JSONArray(prefs.getString("recents", "[]") ?: "[]")
        (0 until arr.length()).map { index ->
            val obj = arr.getJSONObject(index)
            MediaEntry(obj.getString("name"), obj.getString("uri"), false)
        }
    }.getOrDefault(emptyList())

    fun addRecent(entry: MediaEntry) {
        val list = (listOf(entry) + recents().filterNot { it.uri == entry.uri }).take(12)
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().put("name", item.name).put("uri", item.uri))
        }
        prefs.edit().putString("recents", arr.toString()).apply()
    }

    fun clearCache(context: Context) {
        runCatching { context.cacheDir.deleteRecursively() }
    }
}

suspend fun browseTree(context: Context, treeUri: Uri): List<MediaEntry> = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val documentId = DocumentsContract.getTreeDocumentId(treeUri)
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    val result = mutableListOf<MediaEntry>()

    resolver.query(
        children,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ),
        null,
        null,
        null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val name = cursor.getString(1) ?: continue
            val mime = cursor.getString(2)
            val isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
            val extension = name.substringAfterLast('.', "").lowercase()
            if (isDirectory || extension in VIDEO_EXTENSIONS) {
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                result += MediaEntry(name, uri.toString(), isDirectory)
            }
        }
    }

    result.sortedWith(compareByDescending<MediaEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
}

suspend fun fetchServerEntries(base: String): List<MediaEntry> = withContext(Dispatchers.IO) {
    val baseUrl = base.trim()
    val urlPath = runCatching { URL(baseUrl) }.getOrNull() ?: return@withContext emptyList()
    val path = urlPath.path.lowercase()
    val extension = path.substringAfterLast('/', "").substringAfterLast('.', "")

    if (extension in VIDEO_EXTENSIONS) {
        val name = urlPath.path.substringAfterLast('/').ifBlank { "Video" }
        return@withContext listOf(MediaEntry(name, baseUrl, false))
    }

    val normalized = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
    val body = runCatching { URL(normalized).readText() }.getOrElse { return@withContext emptyList() }

    Regex("href=[\\\"']([^\\\"'#?]+)[\\\"']", RegexOption.IGNORE_CASE)
        .findAll(body)
        .mapNotNull { match ->
            val href = match.groupValues[1]
            if (href == "../" || href == "./") return@mapNotNull null
            val full = runCatching { URL(URL(normalized), href).toString() }.getOrNull()
                ?: return@mapNotNull null
            val isDirectory = href.endsWith('/')
            val itemExtension = href.substringBefore('?').substringAfterLast('.', "").lowercase()
            if (isDirectory || itemExtension in VIDEO_EXTENSIONS) {
                MediaEntry(href.trimEnd('/'), full, isDirectory)
            } else null
        }
        .toList()
        .distinctBy { it.uri }
        .sortedWith(compareByDescending<MediaEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
}
