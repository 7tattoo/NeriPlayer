package com.tencent.ibg.joox.listentogether.playback

import com.tencent.ibg.joox.data.model.SongItem
import com.tencent.ibg.joox.listentogether.mapping.toListenTogetherTrackOrNull

internal fun SongItem?.isShareableForListenTogether(): Boolean {
    return this?.toListenTogetherTrackOrNull() != null
}

internal fun List<SongItem>.hasShareableListenTogetherTrackAt(index: Int): Boolean {
    return getOrNull(index).isShareableForListenTogether()
}
