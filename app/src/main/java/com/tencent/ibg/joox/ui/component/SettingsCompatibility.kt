package com.tencent.ibg.joox.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HsvPicker(
    onColorChanged: (String) -> Unit,
    initialHex: String = "0061A4"
) {
    com.tencent.ibg.joox.ui.component.settings.HsvPicker(
        onColorChanged = onColorChanged,
        initialHex = initialHex
    )
}

@Composable
fun LanguageSettingItem(
    modifier: Modifier = Modifier,
    onLanguageChanged: (com.tencent.ibg.joox.util.platform.LanguageManager.Language) -> Unit = {}
) {
    com.tencent.ibg.joox.ui.component.settings.LanguageSettingItem(
        modifier = modifier,
        onLanguageChanged = onLanguageChanged
    )
}
