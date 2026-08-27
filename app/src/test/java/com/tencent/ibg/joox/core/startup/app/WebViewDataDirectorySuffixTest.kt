package com.tencent.ibg.joox.core.startup.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewDataDirectorySuffixTest {
    @Test
    fun `uses process segment after colon`() {
        assertEquals(
            "bili.login",
            WebViewDataDirectorySuffix.forProcess("com.tencent.ibg.joox:bili.login")
        )
    }

    @Test
    fun `uses full process name when no colon exists`() {
        assertEquals(
            "com.tencent.ibg.joox",
            WebViewDataDirectorySuffix.forProcess("com.tencent.ibg.joox")
        )
    }

    @Test
    fun `falls back when suffix is blank`() {
        assertEquals("webview", WebViewDataDirectorySuffix.forProcess("com.tencent.ibg.joox:"))
    }

    @Test
    fun `normalizes unsafe suffix characters`() {
        assertEquals(
            "login_process_1",
            WebViewDataDirectorySuffix.forProcess("com.tencent.ibg.joox:login process#1")
        )
    }
}
