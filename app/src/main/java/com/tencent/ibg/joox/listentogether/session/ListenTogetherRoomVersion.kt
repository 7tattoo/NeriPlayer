package com.tencent.ibg.joox.listentogether.session

import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherRoomState

internal fun latestListenTogetherAcceptedRoomVersion(
    lastAppliedRoomVersion: Long,
    currentState: ListenTogetherRoomState?
): Long {
    return maxOf(lastAppliedRoomVersion, currentState?.version ?: -1L)
}
