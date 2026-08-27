package com.tencent.ibg.joox.core.startup.player

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import com.tencent.ibg.joox.data.model.stableKey
import com.tencent.ibg.joox.data.model.SongItem

internal class PlayerStartupHistoryRecorder(
    private val currentSongFlow: StateFlow<SongItem?>,
    private val recordSong: (SongItem) -> Unit,
    startupSongToSkip: SongItem?,
    private val settleDelayMs: Long = DEFAULT_SETTLE_DELAY_MS
) {
    private val startupSongKeyToSkip = startupSongToSkip?.stableKey()

    suspend fun run() {
        val startupSongKey = startupSongKeyToSkip
        var hasLeftStartupSong = startupSongKey == null
        var lastRecordedSongKey: String? = null
        currentSongFlow
            .filterNotNull()
            .collect { song ->
                val songKey = song.stableKey()
                if (!hasLeftStartupSong && songKey == startupSongKey) {
                    return@collect
                }
                hasLeftStartupSong = true
                if (songKey == lastRecordedSongKey) {
                    return@collect
                }
                delay(settleDelayMs)
                if (currentSongFlow.value?.stableKey() != songKey) {
                    return@collect
                }
                recordSong(song)
                lastRecordedSongKey = songKey
            }
    }

    companion object {
        const val DEFAULT_SETTLE_DELAY_MS = 700L
    }
}
