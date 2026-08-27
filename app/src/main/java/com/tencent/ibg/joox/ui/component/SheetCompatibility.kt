package com.tencent.ibg.joox.ui.component

import androidx.compose.ui.Modifier
import com.tencent.ibg.joox.ui.component.sheet.bottomSheetDragBlocker as newBottomSheetDragBlocker
import com.tencent.ibg.joox.ui.component.sheet.bottomSheetScrollGuard as newBottomSheetScrollGuard

fun Modifier.bottomSheetScrollGuard(
    allowDownwardToParent: (() -> Boolean)? = null
): Modifier =
    newBottomSheetScrollGuard(allowDownwardToParent)

fun Modifier.bottomSheetDragBlocker(): Modifier =
    newBottomSheetDragBlocker()
