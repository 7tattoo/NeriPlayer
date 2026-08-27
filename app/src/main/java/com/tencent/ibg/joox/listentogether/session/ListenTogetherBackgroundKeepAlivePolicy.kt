package com.tencent.ibg.joox.listentogether.session

internal fun shouldHoldListenTogetherBackgroundKeepAlive(
    sessionActive: Boolean,
    reconnectEnabled: Boolean,
    applicationInForeground: Boolean
): Boolean {
    return sessionActive && reconnectEnabled && !applicationInForeground
}
