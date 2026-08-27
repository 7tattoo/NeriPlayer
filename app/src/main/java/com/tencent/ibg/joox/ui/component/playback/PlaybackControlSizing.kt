package com.tencent.ibg.joox.ui.component.playback

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tencent.ibg.joox.data.settings.PlaybackControlSize

internal fun PlaybackControlSize.scaleButtonSize(baseSize: Dp): Dp {
    return (baseSize * scale).coerceAtLeast(40.dp)
}

internal fun PlaybackControlSize.scaleIconSize(baseSize: Dp): Dp {
    return (baseSize * scale).coerceAtLeast(18.dp)
}
