package com.alveon.plus.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

private const val PREFS = "alveon_local"
private const val VIDEO_EXTENSIONS = "mp4,mkv,avi,mov,webm,m4v,ts,mpeg,mpg,3gp"

data class MediaEntry(val name: String, val uri: String, val isDirectory: Boolean)
data class ServerEntry(val name: String, val url: String)

class LocalRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun servers(): List<ServerEntry> = runCatching {
        val data = JSONArray(prefs.getString("servers", "[]"))
        (0 until data.length()).map { data.getJSONObject(it).let { obj -> ServerEntry(obj.getString("name"), obj.getString("url")) } }
    }.getOrDefault(emptyList())

    fun saveServer(server: ServerEntry) {
        val all = (servers().filterNot { it.url == server.url } + server)
        prefs.edit().putString("servers", JSONArray().also { arr -> all.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) } }.toString()).apply()
    }

    fun removeServer(server: ServerEntry) { saveServers(servers().filterNot { it == server }) }
    private fun saveServers(items: List<ServerEntry>) {
        prefs.edit().putString("servers", JSONArray().also { arr -> items.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) } }.toString()).apply()
    }

    fun recents(): List<MediaEntry> = runCatching {
        val arr = JSONArray(prefs.getString("recents", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it).let { o -> MediaEntry(o.getString("name"), o.getString("uri"), false) } }
    }.getOrDefault(emptyList())

    fun addRecent(entry: MediaEntry) {
        val list = (listOf(entry) + recents().filterNot { it.uri == entry.uri }).take(12)
        prefs.edit().putString("recents", JSONArray().also { arr -> list.forEach { arr.put(JSONObject().put("name", it.name).put("uri", it.uri)) } }.toString()).apply()
    }

    fun clearCache(context: Context) { runCatching { context.cacheDir.deleteRecursively() } }
}

suspend fun browseTree(context: Context, treeUri: Uri): List<MediaEntry> = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    val result = mutableListOf<MediaEntry>()
    resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val name = cursor.getString(1) ?: continue
            val mime = cursor.getString(2)
            val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
            if (isDir || name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS.split(',')) {
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                result += MediaEntry(name, uri.toString(), isDir)
            }
        }
    }
    result.sortedWith(compareByDescending<MediaEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
}

suspend fun fetchServerEntries(base: String): List<MediaEntry> = withContext(Dispatchers.IO) {
    val normalized = if (base.endsWith('/')) base else "$base/"
    val body = URL(normalized).readText()
    Regex("href=[\\\"']([^\\\"'#?]+)[\\\"']", RegexOption.IGNORE_CASE).findAll(body).mapNotNull { match ->
        val href = match.groupValues[1]
        if (href == "../" || href == "./" || href.startsWith("mailto:")) return@mapNotNull null
        val full = runCatching { URL(URL(normalized), href).toString() }.getOrNull() ?: return@mapNotNull null
        val isDir = href.endsWith('/')
        val ext = href.substringBefore('?').substringAfterLast('.', "").lowercase()
        if (isDir || ext in VIDEO_EXTENSIONS.split(',')) MediaEntry(href.trimEnd('/'), full, isDir) else null
    }.toList().distinctBy { it.uri }
}
