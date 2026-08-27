package com.tencent.ibg.joox.core.startup.safemode

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tencent.ibg.joox.data.auth.bili.BiliCookieRepository
import com.tencent.ibg.joox.data.auth.netease.NeteaseCookieRepository
import com.tencent.ibg.joox.data.auth.web.clearAllWebViewLoginState
import com.tencent.ibg.joox.data.auth.youtube.YouTubeAuthRepository
import com.tencent.ibg.joox.data.settings.BootstrapSettingsSnapshot
import com.tencent.ibg.joox.data.settings.PlaybackPreferenceSnapshot
import com.tencent.ibg.joox.data.settings.ThemePreferenceSnapshot
import com.tencent.ibg.joox.data.settings.dataStore
import com.tencent.ibg.joox.data.settings.persistBootstrapSettingsSnapshot
import com.tencent.ibg.joox.data.settings.persistPlaybackPreferenceSnapshot
import com.tencent.ibg.joox.data.settings.persistThemePreferenceSnapshot

internal class SafeModeResetActions(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun clearAllCookiesAndLoginOptions() {
        withContext(Dispatchers.IO) {
            NeteaseCookieRepository(appContext).clear()
            BiliCookieRepository(appContext).clear()
            YouTubeAuthRepository(appContext).clear()
        }
        clearAllWebViewLoginState(appContext)
    }

    suspend fun resetAppSettings() {
        withContext(Dispatchers.IO) {
            appContext.dataStore.edit { prefs ->
                prefs.clear()
            }
            persistThemePreferenceSnapshot(appContext, ThemePreferenceSnapshot())
            persistBootstrapSettingsSnapshot(appContext, BootstrapSettingsSnapshot())
            persistPlaybackPreferenceSnapshot(appContext, PlaybackPreferenceSnapshot())
        }
    }
}
