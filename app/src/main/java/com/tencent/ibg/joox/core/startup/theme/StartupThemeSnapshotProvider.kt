package com.tencent.ibg.joox.core.startup.theme

import android.content.Context
import com.tencent.ibg.joox.data.settings.ThemePreferenceSnapshot
import com.tencent.ibg.joox.data.settings.readThemePreferenceSnapshotSync

internal object StartupThemeSnapshotProvider {
    fun read(
        context: Context,
        safeModeActive: Boolean
    ): ThemePreferenceSnapshot {
        return if (safeModeActive) {
            ThemePreferenceSnapshot()
        } else {
            readThemePreferenceSnapshotSync(context)
        }
    }
}
