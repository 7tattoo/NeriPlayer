package com.tencent.ibg.joox.listentogether.session

import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherRoomState

internal fun shouldApplyListenTogetherClosedRoomPause(
    state: ListenTogetherRoomState
): Boolean {
    return state.playback.state == "paused"
}
