package com.tencent.ibg.joox.core.player.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import com.tencent.ibg.joox.core.logging.NPLogger
import com.tencent.ibg.joox.core.player.PlayerManager
import com.tencent.ibg.joox.core.player.audio.isBluetoothOutputType
import com.tencent.ibg.joox.core.player.metadata.ExternalBluetoothLyricPayload
import com.tencent.ibg.joox.core.player.metadata.findExternalBluetoothLyricLine
import com.tencent.ibg.joox.core.player.metadata.findFloatingTranslatedLyricLine
import com.tencent.ibg.joox.core.player.metadata.resolveExternalBluetoothLyricPayload
import com.tencent.ibg.joox.data.model.SongItem
import com.tencent.ibg.joox.data.model.sameIdentityAs
import com.tencent.ibg.joox.data.model.stableKey
import com.tencent.ibg.joox.data.settings.resolveLyricDefaultOffsetMs
import com.tencent.ibg.joox.ui.component.lyrics.LyricEntry
import com.tencent.ibg.joox.ui.component.lyrics.matchTranslationsToLineIndices

internal fun PlayerManager.syncExternalBluetoothLyrics(song: SongItem?) {
    externalBluetoothLyricsLoadJob?.cancel()
    externalBluetoothLyricsLoadJob = null
    externalBluetoothTranslationLoadJob?.cancel()
    externalBluetoothTranslationLoadJob = null
    externalBluetoothLyrics = emptyList()
    floatingTranslatedLyrics = emptyList()
    floatingTranslationMatchesByIndex = emptyMap()
    externalBluetoothLyricsSongKey = song?.stableKey()
    clearExternalBluetoothLyricLine()

    if (!shouldProvideExternalLyricLine() || song == null) {
        return
    }

    val songKey = song.stableKey()
    externalBluetoothLyricsLoadJob = ioScope.launch {
        val lyrics = loadExternalLyrics(song)
            .sortedBy { it.startTimeMs }
        currentCoroutineContext().ensureActive()

        val currentSong = _currentSongFlow.value
        if (!shouldProvideExternalLyricLine() || currentSong?.sameIdentityAs(song) != true) {
            return@launch
        }

        externalBluetoothLyricsSongKey = songKey
        externalBluetoothLyrics = lyrics
        updateExternalBluetoothLyricLine(_playbackPositionMs.value, forceNotify = true)
        
        if (shouldProvideExternalTranslatedLyricLine()) {
            startExternalBluetoothTranslationLoad(song, songKey)
        }
    }
}

internal fun PlayerManager.syncExternalTranslatedLyrics(song: SongItem?) {
    externalBluetoothTranslationLoadJob?.cancel()
    externalBluetoothTranslationLoadJob = null
    if (!shouldProvideExternalTranslatedLyricLine()) {
        clearFloatingTranslatedLyricLine()
        updateExternalBluetoothLyricLine(_playbackPositionMs.value)
        return
    }
    if (!shouldProvideExternalLyricLine() || song == null) {
        clearExternalBluetoothLyricLine()
        return
    }

    val songKey = song.stableKey()
    if (externalBluetoothLyricsSongKey != songKey || externalBluetoothLyrics.isEmpty()) {
        syncExternalBluetoothLyrics(song)
        return
    }

    startExternalBluetoothTranslationLoad(song, songKey)
}

private fun PlayerManager.startExternalBluetoothTranslationLoad(
    song: SongItem,
    songKey: String
) {
    externalBluetoothTranslationLoadJob?.cancel()
    externalBluetoothTranslationLoadJob = ioScope.launch {
        val translatedLyrics = loadExternalTranslatedLyrics(song)
            .sortedBy { it.startTimeMs }
        currentCoroutineContext().ensureActive()

        val currentSong = _currentSongFlow.value
        if (
            !shouldProvideExternalTranslatedLyricLine() ||
            currentSong?.sameIdentityAs(song) != true ||
            externalBluetoothLyricsSongKey != songKey
        ) {
            return@launch
        }

        floatingTranslatedLyrics = translatedLyrics
        floatingTranslationMatchesByIndex = matchTranslationsToLineIndices(
            lines = externalBluetoothLyrics,
            translations = translatedLyrics.filter { it.text.isNotBlank() }
        )
        updateExternalBluetoothLyricLine(_playbackPositionMs.value)
    }
}

private suspend fun PlayerManager.loadExternalLyrics(song: SongItem): List<LyricEntry> {
    return try {
        getLyrics(song)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        NPLogger.w(
            "NERI-PlayerManager",
            "external lyrics load failed: song=${song.name}/${song.id}",
            error
        )
        emptyList()
    }
}

private suspend fun PlayerManager.loadExternalTranslatedLyrics(song: SongItem): List<LyricEntry> {
    return try {
        getTranslatedLyrics(song)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        NPLogger.w(
            "NERI-PlayerManager",
            "external translated lyrics load failed: song=${song.name}/${song.id}",
            error
        )
        emptyList()
    }
}

internal fun PlayerManager.updateExternalBluetoothLyricLine(
    positionMs: Long,
    forceNotify: Boolean = false
) {
    if (!shouldProvideExternalLyricLine()) {
        clearExternalBluetoothLyricLine()
        return
    }

    val song = _currentSongFlow.value
    if (song == null || externalBluetoothLyricsSongKey != song.stableKey()) {
        clearExternalBluetoothLyricLine()
        return
    }

    val lyricOffsetMs = resolveLyricDefaultOffsetMs(
        lyricSource = song.matchedLyricSource,
        cloudMusicDefaultOffsetMs = cloudMusicLyricDefaultOffsetMs,
        qqMusicDefaultOffsetMs = qqMusicLyricDefaultOffsetMs
    ) + song.userLyricOffsetMs

    val line = findExternalBluetoothLyricLine(
        lyrics = externalBluetoothLyrics,
        positionMs = positionMs,
        lyricOffsetMs = lyricOffsetMs
    )
    val translatedLine = findFloatingTranslatedLyricLine(
        lyrics = externalBluetoothLyrics,
        translations = floatingTranslatedLyrics,
        positionMs = positionMs,
        lyricOffsetMs = lyricOffsetMs,
        translationMatchesByIndex = floatingTranslationMatchesByIndex
    )

    if (_externalBluetoothLyricLineFlow.value != line) {
        _externalBluetoothLyricLineFlow.value = line
    }
    if (_floatingTranslatedLyricLineFlow.value != translatedLine) {
        _floatingTranslatedLyricLineFlow.value = translatedLine
    }
    val payload = resolveExternalBluetoothLyricPayload(
        lyricEnabled = externalBluetoothLyricsEnabled || dynamicIslandLyricsEnabled,
        translationEnabled = externalBluetoothTranslationEnabled ||
            dynamicIslandLyricsEnabled,
        lyricLine = line,
        translationLine = translatedLine
    )
    // 正常情况下只在 payload 变化时才更新，但歌词加载完成时需要强制通知
    // 以确保车载卡片能获取到最新的 LYRICS_WHOLE（完整歌词）
    if (_externalBluetoothLyricPayloadFlow.value != payload || forceNotify) {
        _externalBluetoothLyricPayloadFlow.value = payload
    }
}

internal fun PlayerManager.clearExternalBluetoothLyricLine() {
    if (_externalBluetoothLyricLineFlow.value != null) {
        _externalBluetoothLyricLineFlow.value = null
    }
    clearFloatingTranslatedLyricLine()
    if (_externalBluetoothLyricPayloadFlow.value != ExternalBluetoothLyricPayload()) {
        _externalBluetoothLyricPayloadFlow.value = ExternalBluetoothLyricPayload()
    }
}

private fun PlayerManager.clearFloatingTranslatedLyricLine() {
    floatingTranslatedLyrics = emptyList()
    floatingTranslationMatchesByIndex = emptyMap()
    if (_floatingTranslatedLyricLineFlow.value != null) {
        _floatingTranslatedLyricLineFlow.value = null
    }
}

private fun PlayerManager.shouldProvideExternalLyricLine(): Boolean {
    return externalBluetoothLyricsEnabled ||
        externalBluetoothTranslationEnabled ||
        dynamicIslandLyricsEnabled ||
        carLyricEnabled ||
        statusBarLyricsEnable ||
        floatingLyricsEnabled
}

private fun PlayerManager.shouldProvideExternalTranslatedLyricLine(): Boolean {
    return shouldProvideExternalTranslatedLyricLine(
        externalBluetoothTranslationEnabled = externalBluetoothTranslationEnabled,
        floatingLyricsEnabled = floatingLyricsEnabled,
        floatingLyricsShowTranslation = floatingLyricsShowTranslation,
        dynamicIslandLyricsEnabled = dynamicIslandLyricsEnabled
    )
}

internal fun shouldProvideExternalTranslatedLyricLine(
    externalBluetoothTranslationEnabled: Boolean,
    floatingLyricsEnabled: Boolean,
    floatingLyricsShowTranslation: Boolean,
    dynamicIslandLyricsEnabled: Boolean
): Boolean {
    return externalBluetoothTranslationEnabled ||
        dynamicIslandLyricsEnabled ||
        (floatingLyricsEnabled && floatingLyricsShowTranslation)
}

internal fun PlayerManager.isExternalBluetoothLyricCadenceActive(): Boolean {
    val deviceType = _currentAudioDevice.value?.type
    val hasBluetoothOutput = deviceType != null && isBluetoothOutputType(deviceType)
    return (dynamicIslandLyricsEnabled || hasBluetoothOutput) &&
        (externalBluetoothLyricsEnabled || externalBluetoothTranslationEnabled || dynamicIslandLyricsEnabled) &&
        externalBluetoothLyrics.isNotEmpty()
}
