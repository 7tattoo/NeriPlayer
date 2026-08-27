package com.tencent.ibg.joox.core.download.storage.tree.cache

internal data class TreeChildNameRefresh(
    val names: Set<String>,
    val isComplete: Boolean
)
