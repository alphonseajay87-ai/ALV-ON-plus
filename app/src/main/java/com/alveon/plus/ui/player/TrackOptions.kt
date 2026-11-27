package com.alveon.plus.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
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
    val extra = if (type == C.TRACK_TYPE_AUDIO) when (format.channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> null
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
