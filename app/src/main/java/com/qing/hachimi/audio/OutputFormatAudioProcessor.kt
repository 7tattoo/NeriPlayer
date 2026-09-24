package com.qing.hachimi.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlin.math.ceil
import kotlin.math.floor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 位深选项（Hachimi 语义：auto 跟随音源，f32 = 32 位浮点） */
const val HACHIMI_BIT_DEPTH_AUTO = "auto"
const val HACHIMI_BIT_DEPTH_16 = "16"
const val HACHIMI_BIT_DEPTH_24 = "24"
const val HACHIMI_BIT_DEPTH_32 = "32"
const val HACHIMI_BIT_DEPTH_FLOAT32 = "f32"

data class PlaybackOutputSettings(
    /** auto/16/24/32/f32 */
    val bitDepth: String = HACHIMI_BIT_DEPTH_AUTO,
    /** 目标输出采样率 Hz；0 = 跟随音源 */
    val sampleRate: Int = 0,
    /** 输出后端：auto/track/aaudio/opensles */
    val backend: String = "auto",
    /** USB DAC 独占模式（AAudio exclusive + 绑定设备） */
    val usbExclusive: Boolean = false,
    /** USB DAC 固定输出（AudioTrack 路径绑定 USB 声卡；Oboe 路径传 deviceId） */
    val usbPinned: Boolean = false
) {
    /** Float32 输出需要 AudioTrack 开 float 模式 */
    val forceFloatOutput: Boolean
        get() = bitDepth == HACHIMI_BIT_DEPTH_FLOAT32

    val needsFormatProcessor: Boolean
        get() = bitDepth != HACHIMI_BIT_DEPTH_AUTO || sampleRate != 0

    /** 独占模式或手动选了 Oboe 后端时走原生 AAudio/OpenSL ES 输出 */
    val wantsOboe: Boolean
        get() = usbExclusive || backend == "aaudio" || backend == "opensles"
}

@UnstableApi
class OutputFormatAudioProcessor(
    /** 提供当前输出设置；每次 onConfigure 时读取，位深/采样率改动逐曲生效 */
    private val settingsProvider: () -> PlaybackOutputSettings
) : BaseAudioProcessor() {

    private val settings: PlaybackOutputSettings
        get() = settingsProvider()

    private var inputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var targetFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (!settings.needsFormatProcessor) return AudioProcessor.AudioFormat.NOT_SET
        if (!isSupportedPcmEncoding(inputAudioFormat.encoding)) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        val outputEncoding = targetEncoding(inputAudioFormat.encoding)
        val outputSampleRate = if (settings.sampleRate <= 0) {
            inputAudioFormat.sampleRate
        } else {
            settings.sampleRate
        }

        inputFormat = inputAudioFormat
        targetFormat = AudioProcessor.AudioFormat(
            outputSampleRate,
            inputAudioFormat.channelCount,
            outputEncoding
        )

        return if (
            inputAudioFormat.sampleRate == targetFormat.sampleRate &&
            inputAudioFormat.encoding == targetFormat.encoding
        ) {
            AudioProcessor.AudioFormat.NOT_SET
        } else {
            targetFormat
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputFormat == AudioProcessor.AudioFormat.NOT_SET ||
            targetFormat == AudioProcessor.AudioFormat.NOT_SET
        ) {
            return
        }

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val inputBytesPerFrame = inputFormat.channelCount * bytesPerSample(inputFormat.encoding)
        val inputFrames = inputBuffer.remaining() / inputBytesPerFrame
        if (inputFrames <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val outputFrames = if (inputFormat.sampleRate == targetFormat.sampleRate) {
            inputFrames
        } else {
            ceil(inputFrames * targetFormat.sampleRate.toDouble() / inputFormat.sampleRate).toInt()
        }
        val outputBytes = outputFrames * targetFormat.channelCount * bytesPerSample(targetFormat.encoding)
        val outputBuffer = replaceOutputBuffer(outputBytes).order(ByteOrder.LITTLE_ENDIAN)

        val inputStart = inputBuffer.position()
        val rateRatio = inputFormat.sampleRate.toDouble() / targetFormat.sampleRate
        for (outputFrame in 0 until outputFrames) {
            val sourceFrame = if (inputFormat.sampleRate == targetFormat.sampleRate) {
                outputFrame.toDouble()
            } else {
                outputFrame * rateRatio
            }
            val baseFrame = floor(sourceFrame).toInt().coerceIn(0, inputFrames - 1)
            val nextFrame = (baseFrame + 1).coerceAtMost(inputFrames - 1)
            val fraction = (sourceFrame - baseFrame).toFloat()

            for (channel in 0 until inputFormat.channelCount) {
                val first = readSample(inputBuffer, inputStart, baseFrame, channel)
                val second = readSample(inputBuffer, inputStart, nextFrame, channel)
                writeSample(outputBuffer, first + (second - first) * fraction)
            }
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    override fun onReset() {
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
        targetFormat = AudioProcessor.AudioFormat.NOT_SET
    }

    private fun readSample(buffer: ByteBuffer, inputStart: Int, frame: Int, channel: Int): Float {
        val offset = inputStart +
            (frame * inputFormat.channelCount + channel) * bytesPerSample(inputFormat.encoding)
        return when (inputFormat.encoding) {
            C.ENCODING_PCM_16BIT -> buffer.getShort(offset) / 32768f
            C.ENCODING_PCM_24BIT -> {
                val unsigned = (buffer.get(offset).toInt() and 0xFF) or
                    ((buffer.get(offset + 1).toInt() and 0xFF) shl 8) or
                    ((buffer.get(offset + 2).toInt() and 0xFF) shl 16)
                val signed = if (unsigned and 0x800000 != 0) unsigned or -0x1000000 else unsigned
                signed / 8388608f
            }
            C.ENCODING_PCM_32BIT -> buffer.getInt(offset) / 2147483648f
            C.ENCODING_PCM_FLOAT -> buffer.getFloat(offset).coerceIn(-1f, 1f)
            else -> 0f
        }
    }

    private fun writeSample(buffer: ByteBuffer, sample: Float) {
        val clamped = sample.coerceIn(-1f, 1f)
        when (targetFormat.encoding) {
            C.ENCODING_PCM_16BIT -> buffer.putShort((clamped * 32767f).toInt().toShort())
            C.ENCODING_PCM_24BIT -> {
                val value = (clamped * 8388607f).toInt()
                buffer.put((value and 0xFF).toByte())
                buffer.put(((value shr 8) and 0xFF).toByte())
                buffer.put(((value shr 16) and 0xFF).toByte())
            }
            C.ENCODING_PCM_32BIT -> buffer.putInt((clamped * 2147483647f).toLong().toInt())
            C.ENCODING_PCM_FLOAT -> buffer.putFloat(clamped)
        }
    }

    private fun targetEncoding(inputEncoding: @C.PcmEncoding Int): @C.PcmEncoding Int =
        when (settings.bitDepth) {
            SettingsManager.BIT_DEPTH_16 -> C.ENCODING_PCM_16BIT
            SettingsManager.BIT_DEPTH_24 -> C.ENCODING_PCM_24BIT
            SettingsManager.BIT_DEPTH_32 -> C.ENCODING_PCM_32BIT
            SettingsManager.BIT_DEPTH_FLOAT32 -> C.ENCODING_PCM_FLOAT
            else -> inputEncoding
        }

    private fun bytesPerSample(encoding: @C.PcmEncoding Int): Int =
        when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> 0
        }

    private fun isSupportedPcmEncoding(encoding: @C.PcmEncoding Int): Boolean =
        encoding == C.ENCODING_PCM_16BIT ||
            encoding == C.ENCODING_PCM_24BIT ||
            encoding == C.ENCODING_PCM_32BIT ||
            encoding == C.ENCODING_PCM_FLOAT
}
