package com.tencent.ibg.joox.core.player.metadata

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
 * File: com.tencent.ibg.joox.core.player.metadata/CarLyricUtils
 * Updated: 2026/9/4
 */

import com.tencent.ibg.joox.ui.component.lyrics.LyricEntry

/**
 * vivo 车机歌词协议常量与纯函数。
 *
 * 覆盖两个互相独立的显示位：
 * 1. **车联投屏（ucar / joviincar）**：整段 LRC 走 [MediaMetadata]，车机自己按
 *    PlaybackState 进度滚动多行。
 * 2. **原子随身听（vivomusicmix 音乐小组件）**：整段 LRC 走
 *    `MediaSession.setExtras()` 的 `lrc_change` 事件，组件自己按进度滚动。
 *
 * 三条铁律（都是踩过的坑，别改）：
 * - **绝不写** `ucar.media.metadata.LYRICS_LINE`：那是单行模式的协议信号，
 *   车机收到就切单行卡片并忽略整段歌词。
 * - **绝不写** `music.media.extras.{LYRIC,LYRIC_IS_ALLOWED,NOTICE_CAR}`：
 *   同样是单行通道，定时推送会把车机卡片反复压回单行。
 * - 歌词未就绪时 **什么都不写**，尤其不能写 `LYRICS_STATUS = 1/2`：
 *   那对车机的语义是「本曲确认无歌词」，会永久退回单行模式。
 */

// ——— 车联投屏（ucar）———
const val UCAR_PREFIX = "ucar.media.metadata."

/** 整段 LRC 文本，车机据此滚动多行歌词。 */
const val METADATA_KEY_UCAR_LYRICS_WHOLE = "${UCAR_PREFIX}LYRICS_WHOLE"

/** 歌词状态，Long 类型；只在确实有整段歌词时写 [UCAR_LYRICS_STATUS_HAS_LYRIC]。 */
const val METADATA_KEY_UCAR_LYRICS_STATUS = "${UCAR_PREFIX}LYRICS_STATUS"

/** 0 = 有歌词。1（无歌词）/ 2（加载失败）属于负状态，本工程从不写。 */
const val UCAR_LYRICS_STATUS_HAS_LYRIC = 0L

// ——— 原子随身听（vivomusicmix）———
/**
 * 原子随身听的服务发现 action，必须同时写进 `MediaBrowserService` 的 intent-filter，
 * 否则组件不会把本 App 识别为可用音乐源。
 */
const val VIVO_MUSIC_WIDGET_SERVICE_ACTION = "com.vivo.musicwidgetmix.support.service"

/** 能力位声明，写在 [android.media.MediaMetadata] 里。 */
const val METADATA_KEY_VIVOMIX_SUPPORT_EVENT = "vivomusicmix.media.metadata.support_event"

/** 31 = 7（基础播控）| 8（歌词）| 16（进度条 / seek）；缺 8 位组件不显示歌词区。 */
const val VIVOMIX_SUPPORT_EVENT_ALL = 31L

/** 注意 vivo 官方把 media 拼成了 meida，必须照抄，写成正确拼写收不到。 */
const val VIVOMIX_ACTION_KEY = "vivomusicmix.meida.extra.key.action"
const val VIVOMIX_ACTION_LRC_CHANGE = "vivomusicmix.extra.lrc_change"

/** 同样是官方 typo：meidia_id。 */
const val VIVOMIX_MEDIA_ID_KEY = "vivomusicmix.extra.key.meidia_id"
const val VIVOMIX_LYRIC_KEY = "vivomusicmix.extra.key.lyric"

/**
 * `lrc_change` 兜底重发间隔。事件语义无需高频推送，25s 重发一次用于覆盖
 * 「车机 / 小组件在播放开始之后才连上」的场景。
 */
internal const val ATOMIC_LYRIC_RESEND_INTERVAL_MS = 25_000L

/**
 * 把歌词行列表拼成标准 LRC：`[mm:ss.SSS]歌词文本`，逐行以 `\n` 分隔。
 * 不推送 ELRC 逐字标签，空行直接跳过。
 */
fun buildCarLyricWhole(lyrics: List<LyricEntry>): String {
    if (lyrics.isEmpty()) return ""

    return buildString {
        for (entry in lyrics) {
            val text = entry.text.trim()
            if (text.isBlank()) continue

            val minutes = (entry.startTimeMs / 60_000L).coerceIn(0, 99)
            val seconds = (entry.startTimeMs % 60_000L) / 1_000L
            val millis = (entry.startTimeMs % 1_000L)
            append("[${"%02d".format(minutes)}:${"%02d".format(seconds)}.${"%03d".format(millis)}]")
            appendLine(text)
        }
    }
}

/**
 * 原子随身听事件指纹：曲目 + 歌词内容。指纹变化即代表需要立刻重发 `lrc_change`，
 * 无需等重发窗口（歌词是异步加载的，切歌那一刻推的必定还没歌词）。
 */
internal fun buildAtomicLyricSignature(mediaId: String, wholeLrc: String): String {
    return "$mediaId|${wholeLrc.length}|${wholeLrc.hashCode()}"
}

/**
 * 廉价前置判断：用于播放进度驱动的周期性兜底路径。
 * 曲目没变且重发窗口未到时直接返回 false，避免白拼一遍整段 LRC。
 */
internal fun shouldRefreshAtomicLyricEvent(
    mediaId: String,
    lastMediaId: String?,
    lastSentAtElapsedRealtimeMs: Long,
    nowElapsedRealtimeMs: Long,
    resendIntervalMs: Long = ATOMIC_LYRIC_RESEND_INTERVAL_MS
): Boolean {
    if (mediaId != lastMediaId) return true
    if (lastSentAtElapsedRealtimeMs <= 0L) return true
    return nowElapsedRealtimeMs - lastSentAtElapsedRealtimeMs >= resendIntervalMs
}

/**
 * 是否真正发送 `lrc_change`。
 *
 * 没有整段歌词时返回 false —— 绝不推空 Bundle，那会把组件已经收到的 extras 清掉。
 */
internal fun shouldSendAtomicLyricEvent(
    wholeLrc: String,
    signature: String,
    lastSignature: String?,
    lastSentAtElapsedRealtimeMs: Long,
    nowElapsedRealtimeMs: Long,
    resendIntervalMs: Long = ATOMIC_LYRIC_RESEND_INTERVAL_MS
): Boolean {
    if (wholeLrc.isEmpty()) return false
    if (signature != lastSignature) return true
    if (lastSentAtElapsedRealtimeMs <= 0L) return true
    return nowElapsedRealtimeMs - lastSentAtElapsedRealtimeMs >= resendIntervalMs
}
