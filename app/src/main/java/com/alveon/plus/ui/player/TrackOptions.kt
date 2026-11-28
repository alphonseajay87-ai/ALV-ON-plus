package com.alveon.plus.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import java.util.Locale

data class TrackOption(val label: String, val selected: Boolean, val onSelect: () -> Unit)

private fun label(format: Format, index: Int, type: Int): String {
    val language = format.language?.takeIf { it.isNotBlank() && it != "und" }
        ?.let { Locale.forLanguageTag(it).displayLanguage.replaceFirstChar { c -> c.uppercase() } }
        ?.takeIf { it.isNotBlank() }
    val base = format.label ?: language ?: "Track ${index + 1}"
    val extra = if (type == C.TRACK_TYPE_AUDIO) {
        val ch = when (format.channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> null
        }
        val codec = when (format.sampleMimeType) {
            MimeTypes.AUDIO_E_AC3_JOC -> "Atmos"
            MimeTypes.AUDIO_TRUEHD -> "TrueHD"
            MimeTypes.AUDIO_E_AC3 -> "DD+"
            MimeTypes.AUDIO_AC3 -> "DD"
            MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
            MimeTypes.AUDIO_DTS -> "DTS"
            else -> null
        }
        listOfNotNull(ch, codec).joinToString(" · ").ifBlank { null }
    } else null
    return listOfNotNull(base, extra).joinToString(" · ")
}

private fun options(player: Player, tracks: Tracks, type: Int): List<TrackOption> {
    val out = mutableListOf<TrackOption>()
    var counter = 0
    for (group in tracks.groups) {
        if (group.type != type) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val g = group
            val trackIndex = i
            out += TrackOption(label(group.getTrackFormat(i), counter++, type), group.isTrackSelected(i)) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(type, false)
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, trackIndex))
                    .build()
            }
        }
    }
    return out
}

fun audioOptions(player: Player, tracks: Tracks): List<TrackOption> = options(player, tracks, C.TRACK_TYPE_AUDIO)

fun subtitleOptions(player: Player, tracks: Tracks): List<TrackOption> {
    val tracksList = options(player, tracks, C.TRACK_TYPE_TEXT)
    val disabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
    val off = TrackOption("Off", disabled || tracksList.none { it.selected }) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }
    return listOf(off) + tracksList.map { if (off.selected) it.copy(selected = false) else it }
}

fun speedOptions(player: Player): List<TrackOption> {
    val current = player.playbackParameters.speed
    return listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).map { v ->
        TrackOption(
            label = if (v == 1f) "Normal" else "${v}×",
            selected = kotlin.math.abs(current - v) < 0.01f,
        ) { player.setPlaybackSpeed(v) }
    }
}

/** Score an audio format so higher = better for cinematic playback. */
fun audioQualityScore(format: Format): Int {
    var score = 0
    // Channels: prefer discrete surround
    score += when (format.channelCount) {
        8 -> 400          // 7.1
        6 -> 300          // 5.1
        in 3..5 -> 150
        2 -> 50
        1 -> 10
        else -> 0
    }
    // Codec / object-based audio
    score += when (format.sampleMimeType) {
        MimeTypes.AUDIO_E_AC3_JOC -> 250   // Dolby Atmos (DD+ JOC)
        MimeTypes.AUDIO_TRUEHD -> 220      // TrueHD (often with Atmos)
        MimeTypes.AUDIO_DTS_HD -> 180
        MimeTypes.AUDIO_E_AC3 -> 120
        MimeTypes.AUDIO_DTS -> 100
        MimeTypes.AUDIO_AC3 -> 80
        MimeTypes.AUDIO_AAC -> 40
        MimeTypes.AUDIO_OPUS, MimeTypes.AUDIO_FLAC -> 30
        else -> 0
    }
    // Mild bitrate preference when available
    if (format.bitrate > 0) score += (format.bitrate / 64_000).coerceAtMost(40)
    return score
}

private fun langMatches(formatLang: String?, preferred: String): Boolean {
    if (preferred.isBlank()) return false
    val fl = formatLang?.lowercase(Locale.US)?.take(2) ?: return false
    val pref = preferred.lowercase(Locale.US).take(2)
    return fl == pref
}

/**
 * Picks the best audio track:
 * 1. Prefer the user's preferred language when set.
 * 2. Among matching (or all) tracks, pick the highest quality score (7.1/Atmos > 5.1 > stereo…).
 * Applies an override so the choice sticks for this playback.
 */
fun applyPreferredAudio(player: Player, tracks: Tracks, preferredLang: String) {
    data class Candidate(val group: Tracks.Group, val index: Int, val format: Format, val score: Int)

    val candidates = mutableListOf<Candidate>()
    for (group in tracks.groups) {
        if (group.type != C.TRACK_TYPE_AUDIO) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val f = group.getTrackFormat(i)
            candidates += Candidate(group, i, f, audioQualityScore(f))
        }
    }
    if (candidates.isEmpty()) return

    val preferred = if (preferredLang.isNotBlank()) {
        candidates.filter { langMatches(it.format.language, preferredLang) }
    } else emptyList()

    val pool = preferred.ifEmpty { candidates }
    val best = pool.maxByOrNull { it.score } ?: return

    // Only override if the currently selected track is worse or different language
    val current = candidates.firstOrNull { it.group.isTrackSelected(it.index) }
    val shouldSwitch = current == null ||
        current.score < best.score ||
        (preferredLang.isNotBlank() && !langMatches(current.format.language, preferredLang) &&
            langMatches(best.format.language, preferredLang))

    if (!shouldSwitch) return

    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setOverrideForType(TrackSelectionOverride(best.group.mediaTrackGroup, best.index))
        .build()
}

/**
 * Applies preferred subtitle language if set. Leaves "Off" alone when preference is empty.
 */
fun applyPreferredSubtitle(player: Player, tracks: Tracks, preferredLang: String) {
    if (preferredLang.isBlank()) return

    data class Candidate(val group: Tracks.Group, val index: Int, val format: Format)

    val candidates = mutableListOf<Candidate>()
    for (group in tracks.groups) {
        if (group.type != C.TRACK_TYPE_TEXT) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            candidates += Candidate(group, i, group.getTrackFormat(i))
        }
    }
    val match = candidates.firstOrNull { langMatches(it.format.language, preferredLang) }
        ?: return

    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .setOverrideForType(TrackSelectionOverride(match.group.mediaTrackGroup, match.index))
        .build()
}
