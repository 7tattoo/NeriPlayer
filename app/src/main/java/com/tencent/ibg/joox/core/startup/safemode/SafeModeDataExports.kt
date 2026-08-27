package com.tencent.ibg.joox.core.startup.safemode

import android.content.Context
import android.net.Uri
import com.tencent.ibg.joox.data.backup.BackupManager
import com.tencent.ibg.joox.data.config.ConfigFileManager

internal class SafeModeDataExports(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun exportConfigBackup(destination: Uri): Result<String> {
        return ConfigFileManager(appContext).exportConfig(destination)
    }

    suspend fun exportPlaylistBackup(destination: Uri): Result<String> {
        return BackupManager(appContext).exportPlaylists(destination)
    }

    fun generateConfigBackupFileName(): String {
        return ConfigFileManager(appContext).generateBackupFileName()
    }

    fun generatePlaylistBackupFileName(): String {
        return BackupManager(appContext).generateBackupFileName()
    }
}
