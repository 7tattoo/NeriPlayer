package com.tencent.ibg.joox.core.player.url

import android.content.Context
import android.net.Uri
import com.tencent.ibg.joox.core.player.model.PlaybackAudioInfo
import com.tencent.ibg.joox.core.player.model.PlaybackAudioSource
import com.tencent.ibg.joox.core.player.model.deriveCodecLabel
import com.tencent.ibg.joox.data.local.media.LocalMediaSupport
import com.tencent.ibg.joox.data.model.SongItem
import com.tencent.ibg.joox.data.local.media.localMediaUri

internal fun buildLocalPlaybackAudioInfo(song: SongItem, context: Context): PlaybackAudioInfo? {
    val localUri = song.localMediaUri() ?: return null
    return buildLocalPlaybackAudioInfo(localUri, context)
}

internal fun buildLocalPlaybackAudioInfo(localUri: Uri, context: Context): PlaybackAudioInfo? {
    return runCatching {
        LocalMediaSupport.inspectQuick(
            context = context,
            uri = localUri,
            includeAudioTrackInfo = true
        )
    }.getOrNull()?.let { details ->
        PlaybackAudioInfo(
            source = PlaybackAudioSource.LOCAL,
            codecLabel = deriveCodecLabel(details.audioMimeType ?: details.mimeType),
            mimeType = details.audioMimeType ?: details.mimeType,
            bitrateKbps = details.bitrateKbps,
            sampleRateHz = details.sampleRateHz,
            bitDepth = details.bitsPerSample,
            channelCount = details.channelCount
        )
    }
}
