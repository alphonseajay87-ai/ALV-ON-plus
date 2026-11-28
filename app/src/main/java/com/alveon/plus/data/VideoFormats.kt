package com.alveon.plus.data

object VideoFormats {
    private val extensions = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "ts", "m2ts", "mts",
        "mpeg", "mpg", "3gp", "wmv", "flv", "ogv", "vob",
    )

    fun isVideo(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in extensions

    fun formatSize(bytes: Long): String = when {
        bytes < 0 -> ""
        bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
        bytes >= 1L shl 20 -> "%d MB".format(bytes shr 20)
        else -> "%d KB".format(bytes shr 10)
    }
}
