package com.tencent.ibg.joox.core.player.metadata

import com.tencent.ibg.joox.ui.component.lyrics.LyricEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarLyricUtilsTest {

    private val lyrics = listOf(
        LyricEntry("intro", startTimeMs = 1_234L, endTimeMs = 2_000L),
        LyricEntry("   ", startTimeMs = 2_000L, endTimeMs = 3_000L),
        LyricEntry(" verse ", startTimeMs = 65_010L, endTimeMs = 70_000L)
    )

    @Test
    fun `buildCarLyricWhole emits standard lrc timestamps and drops blank lines`() {
        assertEquals(
            "[00:01.234]intro\n[01:05.010]verse\n",
            buildCarLyricWhole(lyrics)
        )
    }

    @Test
    fun `buildCarLyricWhole returns empty text without lyrics`() {
        assertEquals("", buildCarLyricWhole(emptyList()))
    }

    @Test
    fun `protocol keys keep the vivo official typos`() {
        // vivo 官方把 media 拼成 meida、media_id 拼成 meidia_id，写成正确拼写收不到事件
        assertEquals("vivomusicmix.meida.extra.key.action", VIVOMIX_ACTION_KEY)
        assertEquals("vivomusicmix.extra.key.meidia_id", VIVOMIX_MEDIA_ID_KEY)
        assertEquals("vivomusicmix.extra.key.lyric", VIVOMIX_LYRIC_KEY)
        assertEquals("vivomusicmix.extra.lrc_change", VIVOMIX_ACTION_LRC_CHANGE)
        assertEquals("vivomusicmix.media.metadata.support_event", METADATA_KEY_VIVOMIX_SUPPORT_EVENT)
        assertEquals("com.vivo.musicwidgetmix.support.service", VIVO_MUSIC_WIDGET_SERVICE_ACTION)
        assertEquals("ucar.media.metadata.LYRICS_WHOLE", METADATA_KEY_UCAR_LYRICS_WHOLE)
        assertEquals("ucar.media.metadata.LYRICS_STATUS", METADATA_KEY_UCAR_LYRICS_STATUS)
    }

    @Test
    fun `support event advertises playback lyrics and seek capabilities`() {
        // 31 = 7 (播控) | 8 (歌词) | 16 (进度条)，缺歌词位组件不显示歌词区
        assertEquals(31L, VIVOMIX_SUPPORT_EVENT_ALL)
        assertEquals(8L, VIVOMIX_SUPPORT_EVENT_ALL and 8L)
        assertEquals(0L, UCAR_LYRICS_STATUS_HAS_LYRIC)
    }

    @Test
    fun `atomic lyric event is skipped without whole lyrics`() {
        assertFalse(
            shouldSendAtomicLyricEvent(
                wholeLrc = "",
                signature = buildAtomicLyricSignature("song", ""),
                lastSignature = null,
                lastSentAtElapsedRealtimeMs = 0L,
                nowElapsedRealtimeMs = 10_000L
            )
        )
    }

    @Test
    fun `atomic lyric event fires immediately when the signature changes`() {
        val lrc = buildCarLyricWhole(lyrics)
        val first = buildAtomicLyricSignature("song-a", lrc)
        assertTrue(
            shouldSendAtomicLyricEvent(
                wholeLrc = lrc,
                signature = first,
                lastSignature = null,
                lastSentAtElapsedRealtimeMs = 0L,
                nowElapsedRealtimeMs = 1_000L
            )
        )

        // 同曲目同歌词、重发窗口未到 -> 不重复推送
        assertFalse(
            shouldSendAtomicLyricEvent(
                wholeLrc = lrc,
                signature = first,
                lastSignature = first,
                lastSentAtElapsedRealtimeMs = 1_000L,
                nowElapsedRealtimeMs = 1_000L + ATOMIC_LYRIC_RESEND_INTERVAL_MS - 1L
            )
        )

        // 切歌（指纹变化）-> 立即推送，不等窗口
        assertTrue(
            shouldSendAtomicLyricEvent(
                wholeLrc = lrc,
                signature = buildAtomicLyricSignature("song-b", lrc),
                lastSignature = first,
                lastSentAtElapsedRealtimeMs = 1_000L,
                nowElapsedRealtimeMs = 1_100L
            )
        )
    }

    @Test
    fun `atomic lyric event resends after the fallback window`() {
        val lrc = buildCarLyricWhole(lyrics)
        val signature = buildAtomicLyricSignature("song", lrc)

        // 兜底重发覆盖「车机/组件在播放开始之后才连上」的场景
        assertTrue(
            shouldSendAtomicLyricEvent(
                wholeLrc = lrc,
                signature = signature,
                lastSignature = signature,
                lastSentAtElapsedRealtimeMs = 1_000L,
                nowElapsedRealtimeMs = 1_000L + ATOMIC_LYRIC_RESEND_INTERVAL_MS
            )
        )
    }

    @Test
    fun `refresh gate stays cheap until the track changes or the window elapses`() {
        assertTrue(
            shouldRefreshAtomicLyricEvent(
                mediaId = "song",
                lastMediaId = null,
                lastSentAtElapsedRealtimeMs = 0L,
                nowElapsedRealtimeMs = 500L
            )
        )
        assertTrue(
            shouldRefreshAtomicLyricEvent(
                mediaId = "song-b",
                lastMediaId = "song-a",
                lastSentAtElapsedRealtimeMs = 5_000L,
                nowElapsedRealtimeMs = 5_100L
            )
        )
        assertFalse(
            shouldRefreshAtomicLyricEvent(
                mediaId = "song",
                lastMediaId = "song",
                lastSentAtElapsedRealtimeMs = 5_000L,
                nowElapsedRealtimeMs = 5_000L + ATOMIC_LYRIC_RESEND_INTERVAL_MS - 1L
            )
        )
        assertTrue(
            shouldRefreshAtomicLyricEvent(
                mediaId = "song",
                lastMediaId = "song",
                lastSentAtElapsedRealtimeMs = 5_000L,
                nowElapsedRealtimeMs = 5_000L + ATOMIC_LYRIC_RESEND_INTERVAL_MS
            )
        )
    }
}
