package com.alveon.plus.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/** Lists videos on an HTTP directory index, a WebDAV share, or a single direct video link. */
class ServerBrowser(private val client: OkHttpClient) {

    suspend fun list(server: ServerEntry, url: String): Result<List<MediaEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val httpUrl = url.trim().toHttpUrl()
            val leaf = httpUrl.pathSegments.lastOrNull().orEmpty()
            if (VideoFormats.isVideo(leaf)) {
                return@runCatching listOf(MediaEntry(leaf, httpUrl.toString(), false))
            }
            val base = if (httpUrl.encodedPath.endsWith("/")) httpUrl
            else httpUrl.newBuilder().addPathSegment("").build()
            val entries = when (server.type) {
                ServerType.WEBDAV -> propfind(base)
                ServerType.HTTP -> httpIndex(base)
            }
            entries.distinctBy { it.uri }
                .sortedWith(compareByDescending<MediaEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
        }.recoverCatching { e ->
            throw IllegalStateException(e.message ?: "Couldn't reach the server", e)
        }
    }

    /** Collects videos a couple of folders deep, for the home screen. */
    suspend fun scanVideos(server: ServerEntry, maxDepth: Int = 2, limit: Int = 100): List<MediaEntry> {
        val out = mutableListOf<MediaEntry>()
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(server.url to 0)
        while (queue.isNotEmpty() && out.size < limit) {
            val (url, depth) = queue.removeFirst()
            val entries = list(server, url).getOrNull() ?: continue
            for (e in entries) {
                if (e.isDirectory) {
                    if (depth < maxDepth) queue.add(e.uri to depth + 1)
                } else if (out.size < limit) {
                    out += e
                }
            }
        }
        return out
    }

    private fun httpIndex(base: HttpUrl): List<MediaEntry> {
        val request = Request.Builder().url(base).header("Accept", "text/html,*/*").build()
        val body = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("Server answered HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
        val out = mutableListOf<MediaEntry>()
        val hrefs = Regex("""href\s*=\s*["']([^"'#]+)["']""", RegexOption.IGNORE_CASE)
        for (m in hrefs.findAll(body)) {
            val href = m.groupValues[1]
            if (href.startsWith("?") || href.startsWith("mailto:") || href.startsWith("javascript:")) continue
            val resolved = base.resolve(href) ?: continue
            if (resolved.host != base.host) continue
            val path = resolved.encodedPath
            if (!path.startsWith(base.encodedPath) || path == base.encodedPath) continue
            val name = resolved.pathSegments.lastOrNull { it.isNotEmpty() } ?: continue
            val isDir = path.endsWith("/")
            if (isDir || VideoFormats.isVideo(name)) {
                out += MediaEntry(name, resolved.newBuilder().query(null).build().toString(), isDir)
            }
        }
        return out
    }

    private fun propfind(base: HttpUrl): List<MediaEntry> {
        val payload = """<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/><d:getcontentlength/></d:prop></d:propfind>"""
        val request = Request.Builder()
            .url(base)
            .method("PROPFIND", payload.toRequestBody("application/xml".toMediaType()))
            .header("Depth", "1")
            .build()
        val xml = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("Server answered HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }

        val out = mutableListOf<MediaEntry>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var href: String? = null
        var isCollection = false
        var length = -1L
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfter(':').lowercase()) {
                    "response" -> { href = null; isCollection = false; length = -1L }
                    "href" -> href = parser.nextText().trim()
                    "collection" -> isCollection = true
                    "getcontentlength" -> length = parser.nextText().trim().toLongOrNull() ?: -1L
                }
                XmlPullParser.END_TAG -> if (parser.name.substringAfter(':').lowercase() == "response") {
                    val resolved = href?.let { base.resolve(it) }
                    if (resolved != null && resolved.encodedPath.trimEnd('/') != base.encodedPath.trimEnd('/')) {
                        val name = resolved.pathSegments.lastOrNull { it.isNotEmpty() }
                        if (name != null && (isCollection || VideoFormats.isVideo(name))) {
                            out += MediaEntry(name, resolved.toString(), isCollection, length)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return out
    }
}
