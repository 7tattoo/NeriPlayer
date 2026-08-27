package com.tencent.ibg.joox.core.startup.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProcessClassifierTest {
    @Test
    fun `matches configured main process`() {
        assertTrue(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.tencent.ibg.joox",
                configuredMainProcessName = "com.tencent.ibg.joox",
                packageName = "com.tencent.ibg.joox"
            )
        )
    }

    @Test
    fun `falls back to package name when configured process is blank`() {
        assertTrue(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.tencent.ibg.joox",
                configuredMainProcessName = "",
                packageName = "com.tencent.ibg.joox"
            )
        )
    }

    @Test
    fun `detects secondary process`() {
        assertFalse(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.tencent.ibg.joox:web_login",
                configuredMainProcessName = "com.tencent.ibg.joox",
                packageName = "com.tencent.ibg.joox"
            )
        )
    }
}
