package com.tencent.ibg.joox.core.player.effects

import android.media.audiofx.LoudnessEnhancer
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
import com.tencent.ibg.joox.core.player.model.normalizePlaybackLoudnessGainMb
import com.tencent.ibg.joox.core.player.model.normalizePlaybackPitch
import com.tencent.ibg.joox.core.player.model.normalizePlaybackSpeed
import com.tencent.ibg.joox.core.player.model.normalizePlaybackVolumeBalance
import com.tencent.ibg.joox.core.player.model.resolvePlaybackEqualizerBandLevelsMb
import com.tencent.ibg.joox.core.player.engine.HachimiAudioBridge
import com.tencent.ibg.joox.core.player.engine.PlaybackVolumeBalanceState
import com.tencent.ibg.joox.core.player.engine.PlaybackVolumeNormalizationState
import com.tencent.ibg.joox.core.logging.NPLogger
import com.qing.hachimi.dsp.TenBandEqualizer

/**
 * 统一管理倍速, 音调和均衡器, 避免这些逻辑散在 PlayerManager 里
 *
 * 均衡器部分已迁移到 Hachimi 软件 DSP（EqualizerAudioProcessor 单例），
 * 通过 HachimiAudioBridge.applyEqualizer() 驱动，不依赖系统 android.media.audiofx.Equalizer。
 * 响度增强（LoudnessEnhancer）仍使用系统 API。
 */
class PlaybackEffectsController {
    companion object {
        private const val TAG = "PlaybackEffects"

        /** Hachimi 10 段图形式均衡器的固定中心频率（与 TenBandEqualizer.CENTER_FREQUENCIES 一致） */
        val HACHIMI_CENTER_FREQUENCIES_HZ: List<Int> = TenBandEqualizer.CENTER_FREQUENCIES
    }

    private var player: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var loudnessEnhancerSessionId: Int? = null
    private var config = PlaybackSoundConfig()
    private var currentAudioSessionId: Int? = null

    /** 用 Hachimi 的已知 10 段固定频点替换运行时查询的系统 Equalizer 频点 */
    private val lastKnownBandCentersHz: MutableList<Int> = HACHIMI_CENTER_FREQUENCIES_HZ.toMutableList()
    private var lastKnownBandLevelRangeMb = DEFAULT_EQUALIZER_BAND_LEVEL_RANGE_MB
    private var lastEqualizerAvailable = true   // 软件 DSP 始终可用
    private var lastLoudnessEnhancerAvailable = false
    private var lastAppliedEqualizerLevels: List<Int> = emptyList()
    private var lastAppliedEqualizerEnabled = false

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
            pitch = normalizePlaybackPitch(newConfig.pitch),
            loudnessGainMb = normalizePlaybackLoudnessGainMb(newConfig.loudnessGainMb),
            volumeBalance = normalizePlaybackVolumeBalance(newConfig.volumeBalance)
        )
        if (
            previousConfig.speed != config.speed ||
            previousConfig.pitch != config.pitch
        ) {
            applyPlaybackParameters()
        }
        if (
            previousConfig.equalizerEnabled != config.equalizerEnabled ||
            previousConfig.presetId != config.presetId ||
            previousConfig.customBandLevelsMb != config.customBandLevelsMb
        ) {
            applyEqualizer()
        }
        if (previousConfig.loudnessGainMb != config.loudnessGainMb) {
            applyLoudnessEnhancer()
        }
        PlaybackVolumeBalanceState.update(config.volumeBalance)
        PlaybackVolumeNormalizationState.updateEnabled(config.volumeNormalizationEnabled)
        return buildState()
    }

    @OptIn(UnstableApi::class)
    fun onAudioSessionIdChanged(audioSessionId: Int?): PlaybackSoundState {
        val normalizedSessionId = audioSessionId
            ?.takeIf { it != C.AUDIO_SESSION_ID_UNSET && it > 0 }
        if (currentAudioSessionId != normalizedSessionId) {
            currentAudioSessionId = normalizedSessionId
            if (loudnessEnhancerSessionId != normalizedSessionId) {
                releaseLoudnessEnhancer()
            }
        }
        applyLoudnessEnhancer()
        return buildState()
    }

    fun release(): PlaybackSoundState {
        releaseLoudnessEnhancer()
        PlaybackVolumeBalanceState.update(0f)
        PlaybackVolumeNormalizationState.updateEnabled(false)
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
    private fun applyEqualizer() {
        if (!config.equalizerEnabled) {
            // EQ 关闭：通知 DSP 旁路
            HachimiAudioBridge.applyEqualizer(
                enabled = false,
                bandGainsDb = FloatArray(TenBandEqualizer.BAND_COUNT) { 0f },
                masterGainDb = 0f,
                bassGainDb = 0f,
                trebleGainDb = 0f
            )
            lastAppliedEqualizerEnabled = false
            lastAppliedEqualizerLevels = emptyList()
            NPLogger.d(TAG, "applyEqualizer(): disabled (Hachimi DSP)")
            return
        }

        // 使用 Hachimi 10 段固定频点解析预设/自定义增益
        val bandLevelsMb = resolvePlaybackEqualizerBandLevelsMb(
            presetId = config.presetId,
            customBandLevelsMb = config.customBandLevelsMb,
            bandCentersHz = lastKnownBandCentersHz,
            bandLevelRangeMb = lastKnownBandLevelRangeMb
        )

        // 转换成 dB (mb → dB * 100 → dB)
        val gainsDb = bandLevelsMb.map { it / 100f }.toFloatArray()

        HachimiAudioBridge.applyEqualizer(
            enabled = true,
            bandGainsDb = gainsDb,
            masterGainDb = 0f,
            bassGainDb = 0f,
            trebleGainDb = 0f
        )

        lastAppliedEqualizerEnabled = true
        lastAppliedEqualizerLevels = bandLevelsMb

        NPLogger.d(
            TAG,
            "applyEqualizer(): enabled Hachimi DSP, preset=${config.presetId}, " +
                "gainsDb=${gainsDb.joinToString(prefix = "[", postfix = "]") { "%.1f".format(it) }}"
        )
    }

    // ========================================================================
    // 系统 LoudnessEnhancer（Hachimi DSP 无直接对应，保留系统 API）
    // ========================================================================

    private fun applyLoudnessEnhancer() {
        val sessionId = currentAudioSessionId ?: run {
            releaseLoudnessEnhancer()
            return
        }
        if (config.loudnessGainMb <= 0 && loudnessEnhancer == null) {
            lastLoudnessEnhancerAvailable = false
            return
        }

        val enhancer = ensureLoudnessEnhancer(sessionId) ?: run {
            lastLoudnessEnhancerAvailable = false
            return
        }

        runCatching {
            enhancer.setTargetGain(config.loudnessGainMb)
            enhancer.enabled = config.loudnessGainMb > 0
            lastLoudnessEnhancerAvailable = true
            NPLogger.d(
                TAG,
                "applyLoudnessEnhancer(): sessionId=$sessionId, gainMb=${config.loudnessGainMb}, enabled=${config.loudnessGainMb > 0}"
            )
        }.onFailure {
            lastLoudnessEnhancerAvailable = false
            NPLogger.e(TAG, "applyLoudnessEnhancer(): failed", it)
        }
    }

    private fun ensureLoudnessEnhancer(sessionId: Int): LoudnessEnhancer? {
        val existing = loudnessEnhancer
        if (existing != null && loudnessEnhancerSessionId == sessionId) {
            return existing
        }

        releaseLoudnessEnhancer()
        val created = runCatching {
            LoudnessEnhancer(sessionId).apply { enabled = false }
        }.getOrNull() ?: return null
        NPLogger.d(TAG, "ensureLoudnessEnhancer(): created enhancer for sessionId=$sessionId")
        loudnessEnhancer = created
        loudnessEnhancerSessionId = sessionId
        return created
    }

    private fun releaseLoudnessEnhancer() {
        runCatching { loudnessEnhancer?.enabled = false }
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        loudnessEnhancerSessionId = null
        lastLoudnessEnhancerAvailable = false
    }

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
            loudnessGainMb = config.loudnessGainMb,
            volumeBalance = config.volumeBalance,
            volumeNormalizationEnabled = config.volumeNormalizationEnabled,
            equalizerEnabled = config.equalizerEnabled,
            presetId = config.presetId,
            bands = bands,
            bandLevelRangeMb = lastKnownBandLevelRangeMb,
            audioSessionId = currentAudioSessionId,
            equalizerAvailable = lastEqualizerAvailable,     // 软件 DSP 始终可用
            loudnessEnhancerAvailable = lastLoudnessEnhancerAvailable && currentAudioSessionId != null
        )
    }
}