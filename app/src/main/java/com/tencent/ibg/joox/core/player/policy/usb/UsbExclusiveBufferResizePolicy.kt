package com.tencent.ibg.joox.core.player.policy.usb

import com.tencent.ibg.joox.data.settings.normalizeUsbExclusiveBackgroundBufferMs
import com.tencent.ibg.joox.data.settings.normalizeUsbExclusiveForegroundBufferMs

internal fun shouldApplyActiveUsbBufferResize(
    streaming: Boolean,
    currentBufferMs: Int,
    targetBufferMs: Int
): Boolean {
    if (streaming) return false
    return targetBufferMs != currentBufferMs
}

internal fun usbExclusiveTransferWindowDurationMs(
    bufferDurationMs: Int,
    appInForeground: Boolean
): Int {
    return if (appInForeground) {
        normalizeUsbExclusiveForegroundBufferMs(bufferDurationMs)
    } else {
        normalizeUsbExclusiveBackgroundBufferMs(bufferDurationMs)
    }
}
