package com.tencent.ibg.joox.data.local.database.store

import com.tencent.ibg.joox.data.history.PlayedEntry
import com.tencent.ibg.joox.data.local.database.entity.PlayHistoryEntity
import com.tencent.ibg.joox.data.local.database.entity.toEntity
import com.tencent.ibg.joox.data.model.SongIdentity
import com.tencent.ibg.joox.data.model.stableKey

internal class PlayHistoryRoomMapper {
    fun toEntities(entries: List<PlayedEntry>): List<PlayHistoryEntity> {
        return entries.map(PlayedEntry::toEntity)
    }

    fun toDomain(entries: List<PlayHistoryEntity>): List<PlayedEntry> {
        return entries
            .sortedWith(compareByDescending<PlayHistoryEntity> { it.playedAt }.thenBy { it.identityKey })
            .map { it.toDomain() }
    }

    fun validateRoundTrip(entries: List<PlayedEntry>): Boolean {
        val normalized = entries.sortedWith(
            compareByDescending<PlayedEntry> { it.playedAt }.thenBy {
                SongIdentity(it.id, it.album, it.localFilePath ?: it.mediaUri).stableKey()
            }
        )
        return toDomain(toEntities(entries)) == normalized
    }

    private fun PlayHistoryEntity.toDomain(): PlayedEntry {
        val expectedIdentity = SongIdentity(
            id = identityId,
            album = identityAlbum,
            mediaUri = identityMediaUri
        ).stableKey()
        check(expectedIdentity == identityKey) {
            "Play history identity mismatch: expected=$expectedIdentity actual=$identityKey"
        }
        return PlayedEntry(
            id = id,
            name = name,
            artist = artist,
            album = album,
            albumId = albumId,
            durationMs = durationMs,
            resumePositionMs = resumePositionMs,
            coverUrl = coverUrl,
            mediaUri = mediaUri,
            matchedLyric = matchedLyric,
            matchedTranslatedLyric = matchedTranslatedLyric,
            customCoverUrl = customCoverUrl,
            customName = customName,
            customArtist = customArtist,
            originalName = originalName,
            originalArtist = originalArtist,
            originalCoverUrl = originalCoverUrl,
            originalLyric = originalLyric,
            originalTranslatedLyric = originalTranslatedLyric,
            localFileName = localFileName,
            localFilePath = localFilePath,
            channelId = channelId,
            audioId = audioId,
            subAudioId = subAudioId,
            sourceStableKey = sourceStableKey,
            playedAt = playedAt
        )
    }
}
