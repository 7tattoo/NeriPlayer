package com.tencent.ibg.joox.core.player.metadata

import com.tencent.ibg.joox.ui.component.lyrics.LyricEntry

/**
 * 车载歌词键常量 — vivo 智能车联协议
 *
 * 参考 vivo 车机 MediaSession 歌词读取协议：
 * - 裸键 + ucar.media.metadata. 前缀双写 LYRICS_LINE / LYRICS_WHOLE / LYRICS_STATUS
 * - setExtras 写 music.media.extras.LYRIC / LYRIC_IS_ALLOWED / NOTICE_CAR
 * - vivomusicmix 协议键（供 vivo 智能车载 App 投屏使用）
 */

// ——— Metadata 裸键 ———
const val METADATA_KEY_LYRICS_LINE = "LYRICS_LINE"
const val METADATA_KEY_LYRICS_WHOLE = "LYRICS_WHOLE"
const val METADATA_KEY_LYRICS_STATUS = "LYRICS_STATUS"

// ——— ucar 前缀键 ———
const val UCAR_PREFIX = "ucar.media.metadata."
const val METADATA_KEY_UCAR_LYRICS_LINE = "${UCAR_PREFIX}LYRICS_LINE"
const val METADATA_KEY_UCAR_LYRICS_WHOLE = "${UCAR_PREFIX}LYRICS_WHOLE"
const val METADATA_KEY_UCAR_LYRICS_STATUS = "${UCAR_PREFIX}LYRICS_STATUS"

// ——— Extras 键 ———
const val EXTRA_LYRIC = "music.media.extras.LYRIC"
const val EXTRA_LYRIC_IS_ALLOWED = "music.media.extras.LYRIC_IS_ALLOWED"
const val EXTRA_NOTICE_CAR = "music.media.extras.NOTICE_CAR"

// ——— vivomusicmix 协议键 ———
const val VIVOMIX_ACTION_KEY = "vivomusicmix.meida.extra.key.action" // 注意: media 拼写与 vivo 官方一致
const val VIVOMIX_ACTION_LRC_CHANGE = "vivomusicmix.extra.lrc_change"
const val VIVOMIX_MEDIA_ID_KEY = "vivomusicmix.extra.key.meidia_id"
const val VIVOMIX_LYRIC_KEY = "vivomusicmix.extra.key.lyric"

// LYRICS_STATUS 值
const val LYRIC_STATUS_HAS_LYRIC = "0"
const val LYRIC_STATUS_NO_LYRIC = "1"

/**
 * 根据不同歌词行列表构建标准 LRC 字符串。
 * 格式: [mm:ss.xxx]歌词文本
 * 不推送 ELRC 逐字标签。
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
 * 获取当前单行歌词（取当前播放位置的歌词行文本）。
 * 如果无歌词返回空字符串。
 */
internal fun resolveCarLyricLine(
    lyrics: List<LyricEntry>,
    positionMs: Long,
    lyricOffsetMs: Long = 0L
): String {
    if (lyrics.isEmpty()) return ""

    val targetTimeMs = (positionMs + lyricOffsetMs).coerceAtLeast(0L)
    var result = ""
    for (entry in lyrics) {
        if (entry.startTimeMs <= targetTimeMs) {
            // 只有文本非空的行才视为有效歌词行
            if (entry.text.trim().isNotBlank()) {
                result = entry.text.trim()
            }
        } else {
            break
        }
    }
    return result
}

/**
 * 检测当前歌词是否有效（有人可读的歌词文本）。
 */
internal fun hasValidLyricContent(lyrics: List<LyricEntry>): Boolean {
    return lyrics.any { it.text.trim().isNotBlank() }
}