package com.tencent.ibg.joox.core.startup.theme

import com.tencent.ibg.joox.data.settings.ThemeMode
import com.tencent.ibg.joox.data.settings.ThemePreferenceSnapshot

internal object StartupThemeResolver {
    fun resolveSnapshotUseDark(
        snapshot: ThemePreferenceSnapshot,
        systemDark: Boolean
    ): Boolean {
        return snapshot.resolveUseDark(systemDark)
    }

    fun resolveModeUseDark(
        mode: ThemeMode,
        systemDark: Boolean
    ): Boolean {
        return mode.resolveUseDark(systemDark)
    }
}
