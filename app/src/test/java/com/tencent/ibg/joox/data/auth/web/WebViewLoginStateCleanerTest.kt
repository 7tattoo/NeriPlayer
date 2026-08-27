package com.tencent.ibg.joox.data.auth.web

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewLoginStateCleanerTest {
    @Test
    fun platformLogoutTargetsOnlyItsLoginProcess() {
        assertEquals(
            listOf(NeteaseWebViewLoginStateClearReceiver::class.java.name),
            remoteWebViewLoginStateClearReceiverNames(setOf(WebLoginPlatform.NETEASE))
        )
        assertEquals(
            listOf(BiliWebViewLoginStateClearReceiver::class.java.name),
            remoteWebViewLoginStateClearReceiverNames(setOf(WebLoginPlatform.BILI))
        )
        assertEquals(
            listOf(YouTubeWebViewLoginStateClearReceiver::class.java.name),
            remoteWebViewLoginStateClearReceiverNames(setOf(WebLoginPlatform.YOUTUBE))
        )
    }

    @Test
    fun fullLoginResetTargetsAllLoginProcesses() {
        assertEquals(
            listOf(
                NeteaseWebViewLoginStateClearReceiver::class.java.name,
                BiliWebViewLoginStateClearReceiver::class.java.name,
                YouTubeWebViewLoginStateClearReceiver::class.java.name
            ),
            remoteWebViewLoginStateClearReceiverNames(WebLoginPlatform.entries.toSet())
        )
    }
}
