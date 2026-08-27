package com.tencent.ibg.joox.listentogether.session

import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherRoomSettings

internal fun ListenTogetherRoomSettings?.normalized(): ListenTogetherRoomSettings {
    return this ?: ListenTogetherRoomSettings()
}
