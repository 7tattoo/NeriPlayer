@file:OptIn(androidx.media3.common.util.UnstableApi::class)

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tencent.ibg.joox.core.player.engine

import com.qing.hachimi.audio.EqualizerAudioProcessor
import com.qing.hachimi.audio.EqualizerSettings
import com.qing.hachimi.audio.OutputFormatAudioProcessor
import com.qing.hachimi.audio.OboeAudioOutput
import com.qing.hachimi.audio.PlaybackOutputSettings

/**
 * Hachimi 音频管线统一入口：
 * 1. 10 段参量均衡器 —— 软件 DSP（EqualizerAudioProcessor 单例，改设置即时生效）
 * 2. 输出位深 / 采样率 —— OutputFormatAudioProcessor（每次 configure 重读设置，逐曲生效）
 * 3. Oboe/AAudio 硬件直通输出路由决策（SwitchableOutputAudioSink 使用）
 * 4. USB DAC 固定输出
 * 5. Offload 决策
 */

const val OBOE_AUDIO_API_UNSPECIFIED = 0
const val OBOE_AUDIO_API_AAUDIO = 1
const val OBOE_AUDIO_API_OPENSL_ES = 2

object HachimiAudioBridge {

    // ========== 均衡器 ==========

    val eqProcessor: EqualizerAudioProcessor = EqualizerAudioProcessor()

    @Volatile
    private var eqSettings: EqualizerSettings = EqualizerSettings()

    @Volatile
    private var equalizerEnabled: Boolean = false

    /** 当前均衡器是否启用（供 AudioProcessor 链判断） */
    fun isEqualizerActive(): Boolean = equalizerEnabled

    fun currentEqualizerSettings(): EqualizerSettings = eqSettings

    /**
     * 由 [PlaybackEffectsController] 在 UI 设置变更时调用。
     * 将 PlaybackSoundConfig（预设+自定义频段）转译为 Hachimi EqualizerSettings，
     * 写入 eqProcessor 即时生效。
     *
     * @param config 当前播放声音配置
     * @param bandLevelsMb 10 段频段的增益值（毫分贝），已由预设/自定义处理后传入
     */
    /**
     * 简化的 applyEqualizer（PlaybackEffectsController 使用）
     * 直接传入各频段增益（分贝），适用于预设/自定义频段已被解析后的场景。
     */
    fun applyEqualizer(
        enabled: Boolean,
        bandGainsDb: FloatArray,
        masterGainDb: Float,
        bassGainDb: Float,
        trebleGainDb: Float
    ) {
        equalizerEnabled = enabled
        eqSettings = EqualizerSettings(
            enabled = enabled,
            bandGainsDb = bandGainsDb,
            masterGainDb = masterGainDb,
            eqQ = EqualizerSettings.EQ_DEFAULT_Q,
            bassGainDb = bassGainDb,
            trebleGainDb = trebleGainDb
        )
        eqProcessor.setSettings(eqSettings)
    }

    /**
     * 完整更新全部均衡器与音效设置（环绕/混响/压缩器/立体声扩展等）。
     * PlaybackSoundSheet 中的更多音效调节项可调用此方法即时生效。
     */
    fun applyFullSettings(settings: EqualizerSettings) {
        equalizerEnabled = settings.enabled
        eqSettings = settings
        eqProcessor.setSettings(settings)
    }

    // ========== 输出设置 ==========

    @Volatile
    private var outputSettings: PlaybackOutputSettings = PlaybackOutputSettings()

    fun currentOutputSettings(): PlaybackOutputSettings = outputSettings

    fun applyOutputSettings(settings: PlaybackOutputSettings) {
        outputSettings = settings
    }

    fun createFormatProcessor(clampOutputTo16Bit: Boolean = false): OutputFormatAudioProcessor =
        OutputFormatAudioProcessor(
            settingsProvider = { outputSettings },
            clampOutputTo16Bit = clampOutputTo16Bit
        )

    // ========== Oboe 路由决策（SwitchableOutputAudioSink 用） ==========

    /**
     * 当前是否应该使用 Oboe 音频输出。
     * 条件：USB 独占或后端选了 AAudio/OpenSL ES，且 native 库已加载
     */
    fun shouldUseOboe(): Boolean {
        if (!nativeLoaded()) return false
        return outputSettings.wantsOboe
    }

    /**
     * 返回 (audioApi, exclusive, deviceId)
     * - audioApi: 0=unspecified, 1=AAudio, 2=OpenSL ES
     * - exclusive: 是否独占模式
     * - deviceId: 目标输出设备 ID（0=default）
     */
    fun currentOboeRouting(): Triple<Int, Boolean, Int> {
        val api = when (outputSettings.backend) {
            "aaudio" -> OBOE_AUDIO_API_AAUDIO
            "opensles" -> OBOE_AUDIO_API_OPENSL_ES
            else -> OBOE_AUDIO_API_UNSPECIFIED
        }
        return Triple(api, outputSettings.usbExclusive, 0)
    }

    // ========== Offload 决策 ==========

    /**
     * PCM offload 是否需要禁用（走完整 DSP 管线）。
     */
    fun offloadRequiresPcmPipeline(): Boolean =
        equalizerEnabled ||
            outputSettings.needsFormatProcessor ||
            outputSettings.wantsOboe

    // ========== 工具 ==========

    private var nativeLoaded = false

    fun nativeLoaded(): Boolean {
        if (nativeLoaded) return true
        nativeLoaded = runCatching { OboeAudioOutput.ensureLoaded() }.getOrDefault(false)
        return nativeLoaded
    }

    /** 重置为默认设置 */
    fun reset() {
        equalizerEnabled = false
        eqSettings = EqualizerSettings()
        outputSettings = PlaybackOutputSettings()
        eqProcessor.setSettings(EqualizerSettings())
    }
}