package com.tencent.ibg.joox.core.player.engine

import android.media.AudioDeviceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.Format
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import com.qing.hachimi.audio.OboeAudioSink
import java.nio.ByteBuffer

/**
 * Hachimi 输出后端切换 sink：AudioTrack（DefaultAudioSink）与 Oboe（AAudio/OpenSL ES）双路。
 *
 * 决策在每个流 configure 时重算（来自 HachimiAudioBridge 快照）——
 * 改动后端/位深/采样率/USB 独占后逐曲生效，无需重建播放器。
 * flush/reset 双路都调用（未配置路径为安全空操作），避免切换后残留 AudioTrack。
 */
@UnstableApi
internal class SwitchableOutputAudioSink(
    private val trackSink: AudioSink,
    private val oboeSink: OboeAudioSink,
    private val decide: () -> Boolean,
    /** 返回 (audioApi, exclusive, deviceId)；每次路由到 Oboe 的 configure 前刷新 */
    private val oboeRoutingProvider: (() -> Triple<Int, Boolean, Int>)? = null,
    /** AudioTrack 路径的 USB DAC 固定输出设备（null = 清除固定） */
    private val preferredDeviceProvider: (() -> android.media.AudioDeviceInfo?)? = null
) : AudioSink {

    @Volatile
    private var useOboe: Boolean = runCatching(decide).getOrDefault(false)

    private fun refreshDecision(): Boolean {
        useOboe = runCatching(decide).getOrDefault(false)
        if (useOboe) {
            runCatching {
                oboeRoutingProvider?.invoke()?.let { (api, exclusive, deviceId) ->
                    oboeSink.updateRouting(api, exclusive, deviceId)
                }
            }
        }
        return useOboe
    }

    private val active: AudioSink
        get() = if (useOboe) oboeSink else trackSink

    // ===== 查询类：实时决策（渲染器先 supportsFormat 再 configure） =====

    override fun supportsFormat(format: Format): Boolean =
        if (refreshDecision()) oboeSink.supportsFormat(format) else trackSink.supportsFormat(format)

    override fun getFormatSupport(format: Format): Int =
        if (refreshDecision()) oboeSink.getFormatSupport(format) else trackSink.getFormatSupport(format)

    override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport =
        if (refreshDecision()) oboeSink.getFormatOffloadSupport(format) else trackSink.getFormatOffloadSupport(format)

    // ===== 流程类 =====

    override fun configure(
        format: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?
    ) {
        refreshDecision()
        // USB DAC 固定输出：AudioTrack 路径每次重配前刷新绑定设备（取消固定时传 null 清除）
        runCatching {
            trackSink.setPreferredDevice(preferredDeviceProvider?.invoke())
        }
        active.configure(format, specifiedBufferSize, outputChannels)
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int
    ): Boolean = active.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)

    override fun play() = active.play()

    override fun pause() = active.pause()

    override fun handleDiscontinuity() = active.handleDiscontinuity()

    override fun playToEndOfStream() = active.playToEndOfStream()

    override fun isEnded(): Boolean = active.isEnded

    override fun hasPendingData(): Boolean = active.hasPendingData()

    override fun getCurrentPositionUs(sourceEnd: Boolean): Long =
        active.getCurrentPositionUs(sourceEnd)

    // ===== 参数类 =====

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) =
        active.setPlaybackParameters(playbackParameters)

    override fun getPlaybackParameters(): PlaybackParameters = active.getPlaybackParameters()

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) =
        active.setSkipSilenceEnabled(skipSilenceEnabled)

    override fun getSkipSilenceEnabled(): Boolean = active.getSkipSilenceEnabled()

    override fun setVolume(volume: Float) = active.setVolume(volume)

    override fun setAudioAttributes(audioAttributes: AudioAttributes) =
        active.setAudioAttributes(audioAttributes)

    override fun getAudioAttributes(): AudioAttributes = active.getAudioAttributes() ?: AudioAttributes.DEFAULT

    override fun getAudioCapabilities(): AudioCapabilities =
        active.getAudioCapabilities() ?: AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES

    override fun setAudioSessionId(audioSessionId: Int) = active.setAudioSessionId(audioSessionId)

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) =
        active.setAuxEffectInfo(auxEffectInfo)

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) =
        active.setOutputStreamOffsetUs(outputStreamOffsetUs)

    override fun getAudioTrackBufferSizeUs(): Long = active.getAudioTrackBufferSizeUs()

    override fun enableTunnelingV21() = active.enableTunnelingV21()

    override fun disableTunneling() = active.disableTunneling()

    @RequiresApi(29)
    override fun setOffloadMode(offloadMode: Int) = active.setOffloadMode(offloadMode)

    @RequiresApi(29)
    override fun setOffloadDelayPadding(delayUs: Int, paddingUs: Int) =
        active.setOffloadDelayPadding(delayUs, paddingUs)

    // ===== 监听/环境 =====

    override fun setListener(listener: AudioSink.Listener) = active.setListener(listener)

    override fun setPlayerId(playerId: PlayerId?) = active.setPlayerId(playerId)

    override fun setClock(clock: Clock) = active.setClock(clock)

    override fun setVirtualDeviceId(virtualDeviceId: Int) = active.setVirtualDeviceId(virtualDeviceId)

    /** 固定输出仅对 AudioTrack 路径有意义；Oboe 路径通过 deviceId 决策处理 */
    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) =
        trackSink.setPreferredDevice(audioDeviceInfo)

    // ===== 生命周期：双路都调（未配置路径为安全空操作） =====

    override fun flush() {
        trackSink.flush()
        oboeSink.flush()
    }

    override fun reset() {
        trackSink.reset()
        oboeSink.reset()
    }

    override fun release() {
        runCatching { trackSink.release() }
        runCatching { oboeSink.release() }
    }
}
