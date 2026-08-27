package com.tencent.ibg.joox.core.player.policy.usb

internal fun shouldPromptForUsbExclusiveBackgroundPermission(
    usbExclusiveEnabled: Boolean,
    appResumed: Boolean,
    promptSuppressed: Boolean,
    backgroundBehaviorAllowed: Boolean,
    promptHandledInCurrentSession: Boolean
): Boolean {
    return usbExclusiveEnabled &&
        appResumed &&
        !promptSuppressed &&
        !backgroundBehaviorAllowed &&
        !promptHandledInCurrentSession
}
