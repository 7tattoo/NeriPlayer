package com.tencent.ibg.joox.core.player.quality

import com.tencent.ibg.joox.core.player.PlayerManager
import com.tencent.ibg.joox.core.player.model.PlaybackAudioSource
import com.tencent.ibg.joox.data.settings.normalizeMobileDataBiliAudioQuality
import com.tencent.ibg.joox.data.settings.normalizeMobileDataNeteaseAudioQuality
import com.tencent.ibg.joox.data.settings.normalizeMobileDataYouTubeAudioQuality
import com.tencent.ibg.joox.data.traffic.TrafficNetworkType
import com.tencent.ibg.joox.data.traffic.currentTrafficNetworkType

internal fun PlayerManager.effectiveNeteaseQuality(): String {
    return resolveTrafficAwareQuality(
        source = PlaybackAudioSource.NETEASE,
        defaultQuality = preferredQuality
    )
}

internal fun PlayerManager.effectiveYouTubeQuality(): String {
    return resolveTrafficAwareQuality(
        source = PlaybackAudioSource.YOUTUBE_MUSIC,
        defaultQuality = youtubePreferredQuality
    )
}

internal fun PlayerManager.effectiveBiliQuality(): String {
    return resolveTrafficAwareQuality(
        source = PlaybackAudioSource.BILIBILI,
        defaultQuality = biliPreferredQuality
    )
}

private fun PlayerManager.resolveTrafficAwareQuality(
    source: PlaybackAudioSource,
    defaultQuality: String
): String {
    if (!isApplicationInitialized()) {
        return defaultQuality
    }
    val networkType = application.currentTrafficNetworkType()
    if (networkType == TrafficNetworkType.WIFI) {
        return defaultQuality
    }
    if (mobileDataFollowDefaultAudioQuality) {
        return defaultQuality
    }

    return when (source) {
        PlaybackAudioSource.NETEASE ->
            normalizeMobileDataNeteaseAudioQuality(mobileDataNeteaseAudioQuality)
        PlaybackAudioSource.YOUTUBE_MUSIC ->
            normalizeMobileDataYouTubeAudioQuality(mobileDataYouTubeAudioQuality)
        PlaybackAudioSource.BILIBILI ->
            normalizeMobileDataBiliAudioQuality(mobileDataBiliAudioQuality)
        else -> defaultQuality
    }
}
