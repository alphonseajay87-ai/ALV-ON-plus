package com.alveon.plus.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks

/** Readable facts about the playing file (resolution, HDR, audio format), read from the MKV itself. */

private fun selectedFormat(tracks: Tracks, type: Int): Format? {
    for (group in tracks.groups) {
        if (group.type != type) continue
        for (i in 0 until group.length) {
            if (group.isTrackSelected(i)) return group.getTrackFormat(i)
        }
    }
    return null
}

fun videoBadges(tracks: Tracks): List<String> {
    val f = selectedFormat(tracks, C.TRACK_TYPE_VIDEO) ?: return emptyList()
    val out = mutableListOf<String>()
    when {
        f.height >= 2000 || f.width >= 3800 -> out += "4K"
        f.height >= 1400 -> out += "1440p"
        f.height >= 1000 -> out += "1080p"
        f.height >= 700 -> out += "720p"
        f.height > 0 -> out += "${f.height}p"
    }
    // Ultra-wide / IMAX-like aspect (common for expanded aspect ratio releases)
    if (f.width > 0 && f.height > 0) {
        val ratio = f.width.toFloat() / f.height
        if (ratio >= 2.2f) out += "IMAX"
    }
    val dolbyVision = f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION || f.codecs?.startsWith("dv") == true
    val transfer = f.colorInfo?.colorTransfer
    when {
        dolbyVision -> out += "Dolby Vision"
        transfer == C.COLOR_TRANSFER_ST2084 -> out += "HDR10"
        transfer == C.COLOR_TRANSFER_HLG -> out += "HLG"
    }
    return out
}

fun audioBadges(tracks: Tracks): List<String> {
    val f = selectedFormat(tracks, C.TRACK_TYPE_AUDIO) ?: return emptyList()
    val name = when (f.sampleMimeType) {
        MimeTypes.AUDIO_E_AC3_JOC -> "Dolby Atmos"
        MimeTypes.AUDIO_E_AC3 -> "Dolby Digital Plus"
        MimeTypes.AUDIO_AC3 -> "Dolby Digital"
        MimeTypes.AUDIO_TRUEHD -> "Dolby TrueHD"
        MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
        MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_FLAC -> "FLAC"
        else -> null
    }
    val channels = when (f.channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> if (f.channelCount > 0) "${f.channelCount}ch" else null
    }
    return listOfNotNull(name, channels)
}

fun formatDuration(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(0)
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
