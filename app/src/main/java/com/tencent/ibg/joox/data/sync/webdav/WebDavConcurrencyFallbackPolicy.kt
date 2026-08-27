package com.tencent.ibg.joox.data.sync.webdav

internal fun shouldAllowUnconditionalWebDavWrite(
    expectedFingerprint: String?,
    currentFingerprint: String?
): Boolean {
    return !expectedFingerprint.isNullOrBlank() &&
        expectedFingerprint == currentFingerprint
}
