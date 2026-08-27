package com.tencent.ibg.joox.core.download.storage.lookup

import com.tencent.ibg.joox.core.download.candidateManagedDownloadBaseNames
import com.tencent.ibg.joox.core.download.storage.naming.ManagedDownloadStorageNaming

internal object ManagedDownloadManagedAudioPolicy {
    fun shouldTreatAudioAsManaged(
        audioName: String,
        metadataAudioNames: Set<String>,
        coverEntryNames: Set<String>,
        lyricEntryNames: Set<String>,
        allowMetadataLessAudio: Boolean
    ): Boolean {
        if (audioName in metadataAudioNames) {
            return true
        }
        if (allowMetadataLessAudio) {
            return true
        }
        val candidateBaseNames = candidateManagedDownloadBaseNames(
            audioName.substringBeforeLast('.', audioName)
        )
        val hasManagedCover = ManagedDownloadStorageNaming
            .buildSidecarCandidateNames(candidateBaseNames)
            .any(coverEntryNames::contains)
        if (hasManagedCover) {
            return true
        }
        return ManagedDownloadStorageNaming.buildLyricCandidateNames(
            songId = null,
            candidateBaseNames = candidateBaseNames,
            kind = ManagedDownloadStorageNaming.LyricKind.ORIGINAL
        ).any(lyricEntryNames::contains) ||
            ManagedDownloadStorageNaming.buildLyricCandidateNames(
                songId = null,
                candidateBaseNames = candidateBaseNames,
                kind = ManagedDownloadStorageNaming.LyricKind.TRANSLATED
            ).any(lyricEntryNames::contains) ||
            ManagedDownloadStorageNaming.buildLyricCandidateNames(
                songId = null,
                candidateBaseNames = candidateBaseNames,
                kind = ManagedDownloadStorageNaming.LyricKind.ROMANIZED
            ).any(lyricEntryNames::contains)
    }
}
