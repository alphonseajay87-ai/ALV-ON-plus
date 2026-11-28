package com.alveon.plus.data

data class ParsedName(
    val title: String,
    val year: String?,
    val isEpisode: Boolean,
    val season: Int? = null,
    val episode: Int? = null,
) {
    val display: String get() = if (year != null) "$title ($year)" else title
}

/** Turns "The.Matrix.1999.1080p.BluRay.x264-GRP.mkv" into title "The Matrix", year "1999". */
object FileNameCleaner {
    private val episode = Regex("""(?i)\bS(\d{1,2})\s?E(\d{1,3})\b""")
    private val episodeAlt = Regex("""(?i)\b(\d{1,2})x(\d{2,3})\b""")
    private val year = Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)""")
    private val brackets = Regex("""\[[^\]]*]""")
    private val junk = Regex(
        """(?i)\b(?:2160p|1080p|1080i|720p|576p|480p|4k|uhd|hdr10?|bluray|blu-ray|bdrip|brrip|webrip|web-dl|webdl|hdtv|dvdrip|""" +
            """x264|x265|h264|h265|hevc|xvid|aac|ac3|eac3|dts|remux|proper|repack|extended|unrated|multi|vff|vostfr|truefrench)\b"""
    )
    private val spaces = Regex("""\s+""")

    fun parse(fileName: String): ParsedName {
        val ext = fileName.substringAfterLast('.', "")
        var s = if (ext.isNotEmpty() && ext.length <= 4 && !ext.contains(' ')) {
            fileName.substringBeforeLast('.')
        } else fileName
        val fallback = s.replace('.', ' ').replace('_', ' ').trim()

        s = s.replace('.', ' ').replace('_', ' ').replace(brackets, " ")

        var isEpisode = false
        var seasonNo: Int? = null
        var episodeNo: Int? = null
        (episode.find(s) ?: episodeAlt.find(s))?.let {
            isEpisode = true
            seasonNo = it.groupValues[1].toIntOrNull()
            episodeNo = it.groupValues[2].toIntOrNull()
            s = s.substring(0, it.range.first)
        }

        var foundYear: String? = null
        year.findAll(s).lastOrNull { it.range.first > 0 }?.let {
            foundYear = it.value
            s = s.substring(0, it.range.first)
        }

        junk.find(s)?.let { s = s.substring(0, it.range.first) }

        val title = s.replace(Regex("[()]"), " ")
            .replace(spaces, " ")
            .trim(' ', '-')
            .ifBlank { fallback.replace(spaces, " ") }
        return ParsedName(title, foundYear, isEpisode, seasonNo, episodeNo)
    }
}
