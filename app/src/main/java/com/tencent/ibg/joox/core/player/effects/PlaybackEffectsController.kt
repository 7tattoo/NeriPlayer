package com.tencent.ibg.joox.core.player.effects

import com.qing.hachimi.dsp.TenBandEqualizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tencent.ibg.joox.core.player.model.DEFAULT_EQUALIZER_BAND_LEVEL_RANGE_MB
import com.tencent.ibg.joox.core.player.model.PlaybackEqualizerBand
import com.tencent.ibg.joox.core.player.model.PlaybackSoundConfig
import com.tencent.ibg.joox.core.player.model.PlaybackSoundState
import com.tencent.ibg.joox.core.player.model.defaultPlaybackEqualizerBands
import com.tencent.ibg.joox.core.player.model.normalizePlaybackPitch
import com.tencent.ibg.joox.core.player.model.normalizePlaybackSpeed
import com.tencent.ibg.joox.core.player.model.resolvePlaybackEqualizerBandLevelsMb
import com.tencent.ibg.joox.core.logging.NPLogger

/**
 * 统一管理倍速, 音调和均衡器, 避免这些逻辑散在 PlayerManager 里
 *
 * 均衡器部分已迁移到 Hachimi 软件 DSP（EqualizerAudioProcessor 单例），
 * 通过 HachimiAudioBridge.applyEqualizer() 驱动，不依赖系统 android.media.audiofx.Equalizer。
 */
class PlaybackEffectsController {
    companion object {
        private const val TAG = "PlaybackEffects"

        /** Hachimi 10 段图形式均衡器的固定中心频率（与 TenBandEqualizer.CENTER_FREQUENCIES 一致） */
        val HACHIMI_CENTER_FREQUENCIES_HZ: List<Int> = TenBandEqualizer.CENTER_FREQUENCIES
    }

    private var player: ExoPlayer? = null
    private var config = PlaybackSoundConfig()
    private var currentAudioSessionId: Int? = null

    /** 用 Hachimi 的已知 10 段固定频点替换运行时查询的系统 Equalizer 频点 */
    private val lastKnownBandCentersHz: MutableList<Int> = HACHIMI_CENTER_FREQUENCIES_HZ.toMutableList()
    private var lastKnownBandLevelRangeMb = DEFAULT_EQUALIZER_BAND_LEVEL_RANGE_MB
    private var lastEqualizerAvailable = true   // 软件 DSP 始终可用

    @OptIn(UnstableApi::class)
    fun attachPlayer(player: ExoPlayer?): PlaybackSoundState {
        this.player = player
        applyPlaybackParameters()
        val sessionId = player?.audioSessionId
        return onAudioSessionIdChanged(sessionId)
    }

    fun updateConfig(newConfig: PlaybackSoundConfig): PlaybackSoundState {
        val previousConfig = config
        config = newConfig.copy(
            speed = normalizePlaybackSpeed(newConfig.speed),
            pitch = normalizePlaybackPitch(newConfig.pitch),        )
        if (
            previousConfig.speed != config.speed ||
            previousConfig.pitch != config.pitch
        ) {
            applyPlaybackParameters()
        }
        return buildState()
    }

    @OptIn(UnstableApi::class)
    fun onAudioSessionIdChanged(audioSessionId: Int?): PlaybackSoundState {
        val normalizedSessionId = audioSessionId
            ?.takeIf { it != C.AUDIO_SESSION_ID_UNSET && it > 0 }
        if (currentAudioSessionId != normalizedSessionId) {
            currentAudioSessionId = normalizedSessionId
        }
        return buildState()
    }

    fun release(): PlaybackSoundState {
        player = null
        currentAudioSessionId = null
        return buildState()
    }

    private fun applyPlaybackParameters() {
        val currentPlayer = player ?: return
        runCatching {
            currentPlayer.playbackParameters = PlaybackParameters(
                config.speed,
                config.pitch
            )
        }
    }

    // ========================================================================
    // Hachimi 软件 DSP 均衡器
    // ========================================================================

    /**
     * 通过 HachimiAudioBridge 将均衡器设置应用到 software DSP 管线。
     * EqualizerAudioProcessor 常驻音频管线，setSettings 即时生效，无需重建播放器。
     */

    // ========================================================================
    // ========================================================================

    // ========================================================================
    // 状态构建
    // ========================================================================

    private fun buildState(): PlaybackSoundState {
        val bandLevels = resolvePlaybackEqualizerBandLevelsMb(
            presetId = config.presetId,
            customBandLevelsMb = config.customBandLevelsMb,
            bandCentersHz = lastKnownBandCentersHz,
            bandLevelRangeMb = lastKnownBandLevelRangeMb
        )

        val bands = lastKnownBandCentersHz.mapIndexed { index, centerFreqHz ->
            PlaybackEqualizerBand(
                index = index,
                centerFreqHz = centerFreqHz,
                levelMb = bandLevels.getOrElse(index) { 0 }
            )
        }

        return PlaybackSoundState(
            speed = config.speed,
            pitch = config.pitch,
            equalizerEnabled = config.equalizerEnabled,
            presetId = config.presetId,
            bands = bands,
            bandLevelRangeMb = lastKnownBandLevelRangeMb,
            audioSessionId = currentAudioSessionId,
            equalizerAvailable = lastEqualizerAvailable,     // 软件 DSP 始终可用
        )
    }
}