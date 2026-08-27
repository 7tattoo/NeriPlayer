package com.tencent.ibg.joox.listentogether

import com.tencent.ibg.joox.listentogether.protocol.ListenTogetherRoomState
import com.tencent.ibg.joox.listentogether.session.resolveListenTogetherJoinAutoPauseCause as resolveSessionJoinAutoPauseCause

internal fun resolveListenTogetherJoinAutoPauseCause(
    autoPauseOnJoin: Boolean,
    role: String?,
    state: ListenTogetherRoomState
): String? {
    return resolveSessionJoinAutoPauseCause(
        autoPauseOnJoin = autoPauseOnJoin,
        role = role,
        state = state
    )
}
