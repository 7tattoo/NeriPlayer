package com.tencent.ibg.joox.core.startup.safemode

internal class SafeModeRecoveryCoordinator(
    private val initializeNormalComponents: () -> Unit,
    private val restoreNormalStartup: () -> Unit
) {
    fun restore() {
        initializeNormalComponents()
        restoreNormalStartup()
    }
}
