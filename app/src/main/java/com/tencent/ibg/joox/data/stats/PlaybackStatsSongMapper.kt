package com.tencent.ibg.joox.data.stats

import com.tencent.ibg.joox.data.model.SongItem

internal fun TrackStat.toPlaybackStatsSongItem(): SongItem = SongItem(
    id = id,
    name = name,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    coverUrl = coverUrl,
    mediaUri = localFilePath ?: mediaUri,
    localFilePath = localFilePath,
    localFileName = localFileName,
    customName = customName,
    customArtist = customArtist,
    customCoverUrl = customCoverUrl
)
