package com.tencent.ibg.joox.listentogether.mapping

import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherChannels
import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherTrack
import com.tencent.ibg.joox.data.model.SongItem

fun SongItem.toListenTogetherTrackOrNull(includeLocal: Boolean = false): ListenTogetherTrack? {
    val channel = resolvedChannelId() ?: return null
    if (channel.equals(ListenTogetherChannels.LOCAL, ignoreCase = true) && !includeLocal) {
        return null
    }

    val audio = resolvedAudioId() ?: return null
    val subAudio = resolvedSubAudioId()
    val playlistContext = resolvedPlaylistContextId()
    return ListenTogetherTrack(
        stableKey = buildStableTrackKey(channel, audio, subAudio, playlistContext),
        channelId = channel,
        audioId = audio,
        subAudioId = subAudio,
        playlistContextId = playlistContext,
        mediaUri = mediaUri,
        streamUrl = streamUrl,
        streamUrls = listOfNotNull(streamUrl),
        name = customName ?: name,
        artist = customArtist ?: artist,
        album = album,
        durationMs = durationMs,
        coverUrl = customCoverUrl ?: coverUrl
    )
}
