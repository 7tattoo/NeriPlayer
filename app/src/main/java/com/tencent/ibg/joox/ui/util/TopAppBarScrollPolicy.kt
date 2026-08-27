package com.tencent.ibg.joox.ui.util

internal fun shouldAllowCollapsingTopAppBar(
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
    collapsedFraction: Float = 0f
): Boolean = canScrollForward || canScrollBackward || collapsedFraction > 0f
