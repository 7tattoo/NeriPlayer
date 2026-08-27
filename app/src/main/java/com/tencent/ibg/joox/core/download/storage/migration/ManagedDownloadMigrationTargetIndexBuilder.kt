package com.tencent.ibg.joox.core.download.storage.migration

import com.tencent.ibg.joox.core.download.ManagedDownloadStorage
import com.tencent.ibg.joox.core.download.storage.COVER_SUBDIRECTORY
import com.tencent.ibg.joox.core.download.storage.LYRIC_SUBDIRECTORY
import com.tencent.ibg.joox.core.download.storage.root.ManagedDownloadRootHandle

internal object ManagedDownloadMigrationTargetIndexBuilder {
    fun build(
        targetRoot: ManagedDownloadRootHandle,
        listChildren: (ManagedDownloadRootHandle) -> List<ManagedDownloadStorage.StoredEntry>,
        findSubdirectories: (ManagedDownloadRootHandle, String, Boolean) -> List<ManagedDownloadRootHandle>
    ): ManagedMigrationTargetIndex {
        val rootEntriesByName = listChildren(targetRoot)
            .associateBy(ManagedDownloadStorage.StoredEntry::name)
        val coverEntriesByName = findSubdirectories(targetRoot, COVER_SUBDIRECTORY, true)
            .flatMap(listChildren)
            .associateBy(ManagedDownloadStorage.StoredEntry::name)
        val lyricEntriesByName = findSubdirectories(targetRoot, LYRIC_SUBDIRECTORY, true)
            .flatMap(listChildren)
            .associateBy(ManagedDownloadStorage.StoredEntry::name)
        return ManagedMigrationTargetIndex(
            rootEntriesByName = rootEntriesByName,
            coverEntriesByName = coverEntriesByName,
            lyricEntriesByName = lyricEntriesByName
        )
    }
}
