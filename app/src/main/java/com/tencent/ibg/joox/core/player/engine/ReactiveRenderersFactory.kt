package com.tencent.ibg.joox.core.player.engine

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
 * File: com.tencent.ibg.joox.core.player/ReactiveRenderersFactory
 * Updated: 2026/9/24 - Hachimi DSP integration (SwitchableOutputAudioSink + EQ)
 */


import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.tencent.ibg.joox.core.player.usb.sink.UsbExclusiveAudioSink
import com.tencent.ibg.joox.core.player.effects.AudioReactive
import com.qing.hachimi.audio.OboeAudioSink

/**
 * 自定义 RenderersFactory:
 * - 注入 TeeAudioProcessor 将 PCM 能量送入 AudioReactive, 供可视化/背景特效使用
 * - 注入 Hachimi 软件 DSP 管线（EqualizerAudioProcessor 单例，EQ/音效即时生效）
 * - 输出后端支持 AudioTrack ↔ Oboe(AAudio/OpenSL ES) 双路切换
 */
@UnstableApi
class ReactiveRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        val volumeNormalization = VolumeNormalizationAudioProcessor()
        val balance = StereoBalanceAudioProcessor()
        val tee = TeeAudioProcessor(AudioReactive.teeSink)

        // Hachimi 软件 DSP 处理器链 —— 常驻管线，空载时自行绕过
        val processors = arrayOf<AudioProcessor>(
            volumeNormalization,
            balance,
            HachimiAudioBridge.createFormatProcessor(),  // 位深/采样率转换
            HachimiAudioBridge.eqProcessor,               // 10 段参量均衡器 + 全部音效
            tee
        )

        val trackSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(processors)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(false)
            .build()

        val oboeSink = OboeAudioSink(
            audioApi = HachimiAudioBridge.OBOE_AUDIO_API_AAUDIO,
            exclusive = false,
            processors = emptyList(),  // 软效果由 trackSink 侧处理器链处理
            deviceId = 0
        )

        val switchableSink = SwitchableOutputAudioSink(
            trackSink = trackSink,
            oboeSink = oboeSink,
            decide = { HachimiAudioBridge.shouldUseOboe() },
            oboeRoutingProvider = {
                HachimiAudioBridge.currentOboeRouting()
            }
        )

        return UsbExclusiveAudioSink(context.applicationContext, switchableSink)
    }
}