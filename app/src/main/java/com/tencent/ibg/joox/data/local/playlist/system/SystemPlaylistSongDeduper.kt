package com.tencent.ibg.joox.data.local.playlist.system

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.tencent.ibg.joox.data.local.playlist.system/SystemPlaylistSongDeduper
 * Updated: 2026/3/23
 */

import com.tencent.ibg.joox.data.local.media.LocalSongSupport
import com.tencent.ibg.joox.data.model.SongIdentity
import com.tencent.ibg.joox.data.model.identity
import com.tencent.ibg.joox.data.model.SongItem

internal fun List<SongItem>.distinctSystemSongs(): List<SongItem> {
    if (size < 2) return this

    return SystemPlaylistSongDeduper(size)
        .apply { addAll(this@distinctSystemSongs) }
        .songs()
}

internal class SystemPlaylistSongDeduper(expectedSongCount: Int) {
    private val initialCapacity = expectedSongCount.coerceIn(0, MAX_INITIAL_CAPACITY)
    private val distinct = ArrayList<SongItem>(initialCapacity)
    private val seenIdentities = HashSet<SongIdentity>(initialCapacity)
    private val seenLocalKeys = HashSet<String>()

    fun addAll(songs: Iterable<SongItem>) {
        songs.forEach(::add)
    }

    fun songs(): List<SongItem> = distinct

    fun takeSongs(): MutableList<SongItem> = distinct

    private fun add(song: SongItem) {
        val identity = song.identity()
        if (identity in seenIdentities) {
            return
        }
        val localKeys = LocalSongSupport.localDuplicateKeys(
            song = song,
            includeMetadataFallback = true
        )
        if (localKeys.none(seenLocalKeys::contains)) {
            distinct += song
            seenIdentities += identity
            seenLocalKeys += localKeys
        }
    }

    private companion object {
        const val MAX_INITIAL_CAPACITY = 4_096
    }
}
