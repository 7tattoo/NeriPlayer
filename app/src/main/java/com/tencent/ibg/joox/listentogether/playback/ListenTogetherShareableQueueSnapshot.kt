package com.tencent.ibg.joox.listentogether.playback

import com.tencent.ibg.joox.core.player.PlayerManager
import com.tencent.ibg.joox.listentogether.mapping.toListenTogetherTrackOrNull
import com.tencent.ibg.joox.listentogether.mapping.withStreamUrls
import com.tencent.ibg.joox.core.player.url.currentListenTogetherShareableStreamUrls
import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherRoomSettings
import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherTrack
import com.tencent.ibg.joox.listentogether.session.normalized
import com.tencent.ibg.joox.data.model.SongItem

internal fun List<SongItem>.toShareableQueueSnapshot(
    currentIndex: Int,
    roomSettings: ListenTogetherRoomSettings? = null,
    includeResolvedStreamUrl: Boolean = true,
    resolvedCurrentStreamUrls: List<String>? = null
): Pair<List<ListenTogetherTrack>, Int> {
    if (isEmpty()) return emptyList<ListenTogetherTrack>() to 0

    val targetSong = getOrNull(currentIndex.coerceIn(0, lastIndex))
    val targetStableKey = targetSong?.toListenTogetherTrackOrNull()?.stableKey
    val canShareResolvedStreamUrls = includeResolvedStreamUrl &&
        roomSettings.normalized().shareAudioLinks
    val currentStreamUrls = if (canShareResolvedStreamUrls) {
        resolvedCurrentStreamUrls ?: PlayerManager.currentListenTogetherShareableStreamUrls()
    } else {
        emptyList()
    }
    val shareableQueue = mapNotNull { song ->
        song.toListenTogetherTrackOrNull()?.let { track ->
            val trackWithoutRawStreamUrls = track.withStreamUrls(emptyList())
            if (canShareResolvedStreamUrls && track.stableKey == targetStableKey) {
                trackWithoutRawStreamUrls.withStreamUrls(currentStreamUrls)
            } else {
                trackWithoutRawStreamUrls
            }
        }
    }.boundedAroundStableKey(targetStableKey)
    if (shareableQueue.isEmpty()) return shareableQueue to 0

    val resolvedCurrentIndex = targetStableKey?.let { stableKey ->
        shareableQueue.indexOfFirst { it.stableKey == stableKey }.takeIf { it >= 0 }
    } ?: 0

    return shareableQueue to resolvedCurrentIndex
}

internal fun List<SongItem>.toShareableShuffleRestoreQueueSnapshot(
    activeQueue: List<ListenTogetherTrack>
): List<ListenTogetherTrack> {
    if (isEmpty() || activeQueue.isEmpty()) return emptyList()
    val remainingCounts = activeQueue.groupingBy { it.stableKey }.eachCount().toMutableMap()
    val restoreQueue = mutableListOf<ListenTogetherTrack>()
    for (song in this) {
        val track = song.toListenTogetherTrackOrNull()?.withStreamUrls(emptyList()) ?: continue
        val remaining = remainingCounts[track.stableKey] ?: continue
        if (remaining == 1) {
            remainingCounts.remove(track.stableKey)
        } else {
            remainingCounts[track.stableKey] = remaining - 1
        }
        restoreQueue += track
    }
    return restoreQueue.takeIf { it.size == activeQueue.size }.orEmpty()
}

internal fun List<ListenTogetherTrack>.mergeCurrentTrack(
    currentIndex: Int,
    currentTrack: ListenTogetherTrack?
): List<ListenTogetherTrack> {
    val replacement = currentTrack ?: return this
    if (currentIndex !in indices) return this
    if (this[currentIndex].stableKey != replacement.stableKey) return this
    if (this[currentIndex] == replacement) return this
    return toMutableList().also { it[currentIndex] = replacement }
}
