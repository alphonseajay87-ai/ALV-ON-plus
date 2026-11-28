package com.alveon.plus.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Browses a folder the user granted through the system picker (USB drives, internal storage). */
object StorageBrowser {

    /** Lists folders and videos in [dir] (or the root of [root] when null). */
    suspend fun list(context: Context, root: Uri, dir: Uri?): List<MediaEntry> = withContext(Dispatchers.IO) {
        val docId = if (dir == null) DocumentsContract.getTreeDocumentId(root)
        else DocumentsContract.getDocumentId(dir)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(root, docId)
        val result = mutableListOf<MediaEntry>()

        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                if (name.startsWith(".")) continue
                val mime = c.getString(2).orEmpty()
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                if (isDir || mime.startsWith("video/") || VideoFormats.isVideo(name)) {
                    val size = if (c.isNull(3)) -1L else c.getLong(3)
                    val modified = if (c.columnCount > 4 && !c.isNull(4)) c.getLong(4) else -1L
                    val uri = DocumentsContract.buildDocumentUriUsingTree(root, id)
                    result += MediaEntry(name, uri.toString(), isDir, size, modified)
                }
            }
        }
        result.sortedWith(compareByDescending<MediaEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    /** Collects videos below [root], a few levels deep, for the home screen row. */
    suspend fun scanVideos(context: Context, root: Uri, maxDepth: Int = 3, limit: Int = 200): List<MediaEntry> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<MediaEntry>()
            val queue = ArrayDeque<Pair<Uri?, Int>>()
            queue.add(null to 0)
            while (queue.isNotEmpty() && out.size < limit) {
                val (dir, depth) = queue.removeFirst()
                val entries = runCatching { list(context, root, dir) }.getOrDefault(emptyList())
                for (e in entries) {
                    if (e.isDirectory) {
                        if (depth < maxDepth) queue.add(Uri.parse(e.uri) to depth + 1)
                    } else if (out.size < limit) {
                        out += e
                    }
                }
            }
            out
        }

    fun displayName(context: Context, root: Uri): String = runCatching {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(root, DocumentsContract.getTreeDocumentId(root))
        context.contentResolver.query(
            docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Storage"
}
