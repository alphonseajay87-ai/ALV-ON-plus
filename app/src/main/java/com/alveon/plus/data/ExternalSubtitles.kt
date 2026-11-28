package com.alveon.plus.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import java.util.Locale

/**
 * Finds external subtitle files sitting next to a video (same base name)
 * and returns MediaItem.SubtitleConfiguration entries for Media3.
 *
 * Supported: .srt, .vtt, .ass, .ssa
 * Works for content:// (SAF / USB) and file:// URIs.
 */
object ExternalSubtitles {

    private val SUB_EXTS = listOf("srt", "vtt", "ass", "ssa")

    fun configurations(context: Context, videoUri: String): List<MediaItem.SubtitleConfiguration> {
        return runCatching {
            val uri = Uri.parse(videoUri)
            when (uri.scheme?.lowercase()) {
                "content" -> fromContentUri(context, uri)
                "file" -> fromFileUri(uri)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun fromFileUri(uri: Uri): List<MediaItem.SubtitleConfiguration> {
        val path = uri.path ?: return emptyList()
        val file = java.io.File(path)
        val parent = file.parentFile ?: return emptyList()
        val base = file.nameWithoutExtension
        return SUB_EXTS.mapNotNull { ext ->
            val candidate = java.io.File(parent, "$base.$ext")
            if (candidate.isFile) {
                MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(candidate))
                    .setMimeType(mimeFor(ext))
                    .setLanguage(guessLanguage(base))
                    .setLabel(ext.uppercase(Locale.US))
                    .build()
            } else null
        }
    }

    private fun fromContentUri(context: Context, videoUri: Uri): List<MediaItem.SubtitleConfiguration> {
        // Only works when the URI is a document inside a tree the user granted.
        if (!DocumentsContract.isDocumentUri(context, videoUri)) return emptyList()

        val docId = DocumentsContract.getDocumentId(videoUri)
        val treeUri = try {
            // Reconstruct a usable tree URI from the document URI when possible.
            val authority = videoUri.authority ?: return emptyList()
            val treeId = DocumentsContract.getTreeDocumentId(videoUri)
            DocumentsContract.buildTreeDocumentUri(authority, treeId)
        } catch (_: Exception) {
            return emptyList()
        }

        val parentId = parentDocumentId(docId) ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)

        val baseName = displayName(context, videoUri)?.substringBeforeLast('.') ?: return emptyList()
        val found = mutableListOf<MediaItem.SubtitleConfiguration>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                if (ext !in SUB_EXTS) continue
                if (!name.startsWith(baseName, ignoreCase = true)) continue
                // Prefer exact base match (Movie.srt) over Movie.en.srt etc., but accept both.
                val subUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                found += MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mimeFor(ext))
                    .setLanguage(guessLanguage(name))
                    .setLabel(name.substringAfterLast('.').uppercase(Locale.US))
                    .build()
            }
        }
        return found
    }

    private fun parentDocumentId(docId: String): String? {
        val idx = docId.lastIndexOf('/')
        return if (idx > 0) docId.substring(0, idx) else null
    }

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    private fun mimeFor(ext: String): String = when (ext) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> MimeTypes.APPLICATION_SUBRIP
    }

    private fun guessLanguage(name: String): String? {
        // Very light heuristic: look for .en. or _en. patterns
        val lower = name.lowercase(Locale.US)
        return when {
            lower.contains(".en.") || lower.endsWith(".en") || lower.contains("_en.") -> "en"
            lower.contains(".es.") || lower.endsWith(".es") || lower.contains("_es.") -> "es"
            lower.contains(".fr.") || lower.endsWith(".fr") || lower.contains("_fr.") -> "fr"
            lower.contains(".de.") || lower.endsWith(".de") || lower.contains("_de.") -> "de"
            lower.contains(".it.") || lower.endsWith(".it") || lower.contains("_it.") -> "it"
            lower.contains(".pt.") || lower.endsWith(".pt") || lower.contains("_pt.") -> "pt"
            lower.contains(".ru.") || lower.endsWith(".ru") || lower.contains("_ru.") -> "ru"
            lower.contains(".ja.") || lower.endsWith(".ja") || lower.contains("_ja.") -> "ja"
            lower.contains(".ko.") || lower.endsWith(".ko") || lower.contains("_ko.") -> "ko"
            lower.contains(".zh.") || lower.endsWith(".zh") || lower.contains("_zh.") -> "zh"
            else -> null
        }
    }
}
