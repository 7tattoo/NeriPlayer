package com.tencent.ibg.joox.core.startup.player

internal data class PlayerStartupBootstrapResult(
    val serviceStart: PlayerStartupServiceStart?
)

internal data class PlayerStartupServiceStart(
    val source: String,
    val forceForeground: Boolean
)
