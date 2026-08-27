package com.tencent.ibg.joox.data.sync.github

import com.tencent.ibg.joox.data.sync.model.SyncData

internal object GitHubSyncUploadPolicy {
    fun shouldUpload(
        remoteData: SyncData?,
        requiresMigrationUpload: Boolean,
        mergedData: SyncData
    ): Boolean {
        if (remoteData == null || requiresMigrationUpload) {
            return true
        }
        return SyncDataChangeDetector.hasDataChanged(remoteData, mergedData)
    }
}
