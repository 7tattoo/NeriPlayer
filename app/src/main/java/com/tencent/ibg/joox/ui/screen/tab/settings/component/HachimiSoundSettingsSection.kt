package com.tencent.ibg.joox.ui.screen.tab.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.tencent.ibg.joox.data.settings.HachimiSoundSettingsStore
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qing.hachimi.audio.HACHIMI_BIT_DEPTH_16
import com.qing.hachimi.audio.HACHIMI_BIT_DEPTH_24
import com.qing.hachimi.audio.HACHIMI_BIT_DEPTH_32
import com.qing.hachimi.audio.HACHIMI_BIT_DEPTH_AUTO
import com.qing.hachimi.audio.HACHIMI_BIT_DEPTH_FLOAT32
import com.tencent.ibg.joox.R
import com.tencent.ibg.joox.data.settings.HachimiSoundSnapshot
import com.tencent.ibg.joox.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import com.tencent.ibg.joox.ui.screen.tab.settings.miuix.MiuixSettingsChoiceRow
import com.tencent.ibg.joox.ui.screen.tab.settings.miuix.MiuixSettingsSlider
import com.tencent.ibg.joox.ui.screen.tab.settings.miuix.MiuixSettingsSwitch
import com.tencent.ibg.joox.ui.screen.tab.settings.miuix.MiuixSettingsTextButton
import com.tencent.ibg.joox.ui.screen.tab.settings.page.MiuixSettingsSectionCard
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Hachimi DSP sound engine settings page (方案 C：唯一音效实现).
// ---------------------------------------------------------------------------

private val EQ_BAND_FREQS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
private val EQ_BAND_RES = intArrayOf(
    R.string.sound_engine_band_31, R.string.sound_engine_band_62, R.string.sound_engine_band_125,
    R.string.sound_engine_band_250, R.string.sound_engine_band_500, R.string.sound_engine_band_1k,
    R.string.sound_engine_band_2k, R.string.sound_engine_band_4k, R.string.sound_engine_band_8k,
    R.string.sound_engine_band_16k
)

@Composable
internal fun HachimiSoundSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { HachimiSoundSettingsStore(context) }
    val uiScope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(HachimiSoundSnapshot.DEFAULT) }
    var snapshotLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { store.settingsFlow.collect { snapshot = it; snapshotLoaded = true } }
    val onSnapshotChange: (HachimiSoundSnapshot) -> Unit = { next ->
        // 首次读取完成前不接受写入，否则默认值会覆盖已持久化的设置。
        if (snapshotLoaded) {
            uiScope.launch { store.update { next } }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ----------------------------------------------------------- 引擎开关 + 参数
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.GraphicEq, title = stringResource(R.string.sound_engine_equalizer))
            SoundSwitchItem(
                icon = Icons.Outlined.GraphicEq,
                title = stringResource(R.string.sound_engine_master_switch),
                description = stringResource(R.string.sound_engine_master_switch_summary),
                checked = snapshot.eqEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqEnabled = it)) }
            )
            SoundSliderRow(
                title = stringResource(R.string.sound_engine_master_gain),
                value = snapshot.eqMasterGainDb,
                valueRange = -12f..12f,
                steps = 23,
                format = { "%.1f dB".format(it) },
                onValueChange = { onSnapshotChange(snapshot.copy(eqMasterGainDb = it)) }
            )
            SoundSliderRow(
                title = stringResource(R.string.sound_engine_q),
                value = snapshot.eqEqQ,
                valueRange = 0.3f..10f,
                onValueChange = { onSnapshotChange(snapshot.copy(eqEqQ = it)) }
            )
            EQ_BAND_FREQS.forEachIndexed { index, _ ->
                SoundSliderRow(
                    title = stringResource(EQ_BAND_RES[index]),
                    value = snapshot.eqBandGainsDb.getOrElse(index) { 0f },
                    valueRange = -12f..12f,
                    steps = 47,
                    format = { "%+.1f dB".format(it) },
                    onValueChange = { v ->
                        val gains = snapshot.eqBandGainsDb.copyOf()
                        gains[index] = v
                        onSnapshotChange(snapshot.copy(eqBandGainsDb = gains))
                    }
                )
            }
            SoundSliderRow(
                title = stringResource(R.string.sound_engine_bass),
                value = snapshot.eqBassGainDb,
                valueRange = -12f..12f,
                steps = 23,
                format = { "%+.1f dB".format(it) },
                onValueChange = { onSnapshotChange(snapshot.copy(eqBassGainDb = it)) }
            )
            SoundSliderRow(
                title = stringResource(R.string.sound_engine_treble),
                value = snapshot.eqTrebleGainDb,
                valueRange = -12f..12f,
                steps = 23,
                format = { "%+.1f dB".format(it) },
                onValueChange = { onSnapshotChange(snapshot.copy(eqTrebleGainDb = it)) }
            )
        }

        // ----------------------------------------------------------- 混响
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.MusicNote, title = stringResource(R.string.sound_engine_reverb))
            val presets = listOf(
                0 to R.string.sound_engine_reverb_off,
                10 to R.string.sound_engine_reverb_studio,
                1 to R.string.sound_engine_reverb_small_room,
                2 to R.string.sound_engine_reverb_medium_room,
                3 to R.string.sound_engine_reverb_large_room,
                4 to R.string.sound_engine_reverb_hall,
                5 to R.string.sound_engine_reverb_church,
                6 to R.string.sound_engine_reverb_plate
            )
            SoundChoiceItem(
                title = stringResource(R.string.sound_engine_reverb_preset),
                options = presets.map { it.first to stringResource(it.second) },
                selected = snapshot.eqReverbPreset,
                onSelect = { value -> onSnapshotChange(snapshot.copy(eqReverbPreset = value)) }
            )
        }

        // ----------------------------------------------------------- 环绕
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.Layers, title = stringResource(R.string.sound_engine_surround))
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_surround360),
                description = stringResource(R.string.sound_engine_surround360_summary),
                checked = snapshot.eqSurround360Enabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqSurround360Enabled = it)) }
            )
            if (snapshot.eqSurround360Enabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_intensity),
                    value = snapshot.eqSurround360Intensity,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqSurround360Intensity = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_rotation_speed),
                    value = snapshot.eqSurround360RotationSpeed,
                    valueRange = 0f..360f,
                    steps = 359,
                    format = { "%.0f°/s".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqSurround360RotationSpeed = it)) }
                )
            }
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_panoramic360),
                description = stringResource(R.string.sound_engine_panoramic360_summary),
                checked = snapshot.eqPanoramic360Enabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqPanoramic360Enabled = it)) }
            )
            if (snapshot.eqPanoramic360Enabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_intensity),
                    value = snapshot.eqPanoramic360Intensity,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqPanoramic360Intensity = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_azimuth),
                    value = snapshot.eqPanoramic360AzimuthDegrees,
                    valueRange = -180f..180f,
                    steps = 359,
                    format = { "%.0f°".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqPanoramic360AzimuthDegrees = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_elevation),
                    value = snapshot.eqPanoramic360ElevationDegrees,
                    valueRange = -90f..90f,
                    steps = 179,
                    format = { "%.0f°".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqPanoramic360ElevationDegrees = it)) }
                )
            }
        }

        // ----------------------------------------------------------- 音色
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.Tune, title = stringResource(R.string.sound_engine_tone))
            SoundSliderRow(
                title = stringResource(R.string.sound_engine_stereo_width),
                value = snapshot.eqStereoWidth,
                valueRange = 0f..2f,
                onValueChange = { onSnapshotChange(snapshot.copy(eqStereoWidth = it)) }
            )
            val bal = snapshot.eqChannelBalance
            val balPct = kotlin.math.abs(bal).roundToInt()
            SoundSliderRow(
                title = stringResource(R.string.sound_engine_channel_balance),
                value = snapshot.eqChannelBalance,
                valueRange = -100f..100f,
                steps = 199,
                valueText = when {
                    balPct == 0 -> stringResource(R.string.sound_engine_balance_center)
                    bal < 0f -> stringResource(R.string.sound_engine_balance_left, balPct)
                    else -> stringResource(R.string.sound_engine_balance_right, balPct)
                },
                onValueChange = { onSnapshotChange(snapshot.copy(eqChannelBalance = it)) }
            )
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_loudness_balance),
                description = stringResource(R.string.sound_engine_loudness_balance_summary),
                checked = snapshot.eqLoudnessBalanceEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqLoudnessBalanceEnabled = it)) }
            )
            if (snapshot.eqLoudnessBalanceEnabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_loudness_amount),
                    value = snapshot.eqLoudnessPercent,
                    valueRange = 0f..100f,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqLoudnessPercent = it)) }
                )
            }
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_dynamic_eq),
                description = stringResource(R.string.sound_engine_dynamic_eq_summary),
                checked = snapshot.eqDynamicEqEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqDynamicEqEnabled = it)) }
            )
            if (snapshot.eqDynamicEqEnabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_intensity),
                    value = snapshot.eqDynamicEqIntensity,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqDynamicEqIntensity = it)) }
                )
                // 齿音消除是动态 EQ 的子处理，仅在动态 EQ 打开时才会进入 DSP 链。
                SoundSwitchItem(
                    title = stringResource(R.string.sound_engine_deesser),
                    checked = snapshot.eqDeEsserAmount > 0f,
                    onCheckedChange = {
                        onSnapshotChange(snapshot.copy(eqDeEsserAmount = if (it) 50f else 0f))
                    }
                )
                if (snapshot.eqDeEsserAmount > 0f) {
                    SoundSliderRow(
                        title = stringResource(R.string.sound_engine_intensity),
                        value = snapshot.eqDeEsserAmount,
                        valueRange = 0f..100f,
                        steps = 99,
                        format = { "%.0f%%".format(it) },
                        onValueChange = { onSnapshotChange(snapshot.copy(eqDeEsserAmount = it)) }
                    )
                    SoundSliderRow(
                        title = stringResource(R.string.sound_engine_deesser_frequency),
                        value = snapshot.eqDeEsserFrequencyHz,
                        valueRange = 4000f..10000f,
                        steps = 59,
                        format = { "%.0f Hz".format(it) },
                        onValueChange = { onSnapshotChange(snapshot.copy(eqDeEsserFrequencyHz = it)) }
                    )
                }
            }
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_moog),
                description = stringResource(R.string.sound_engine_moog_summary),
                checked = snapshot.eqMoogLadderEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqMoogLadderEnabled = it)) }
            )
            if (snapshot.eqMoogLadderEnabled) {
                val moogModes = listOf(
                    0 to R.string.sound_engine_moog_lp24,
                    1 to R.string.sound_engine_moog_lp12,
                    2 to R.string.sound_engine_moog_hp24,
                    3 to R.string.sound_engine_moog_bp12,
                    4 to R.string.sound_engine_moog_notch
                )
                SoundChoiceItem(
                    title = stringResource(R.string.sound_engine_moog_mode),
                    options = moogModes.map { it.first to stringResource(it.second) },
                    selected = snapshot.eqMoogLadderMode,
                    onSelect = { value -> onSnapshotChange(snapshot.copy(eqMoogLadderMode = value)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_moog_cutoff),
                    value = snapshot.eqMoogLadderCutoffHz,
                    valueRange = 20f..20000f,
                    format = { if (it >= 1000f) "%.1f kHz".format(it / 1000f) else "%.0f Hz".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqMoogLadderCutoffHz = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_moog_resonance),
                    value = snapshot.eqMoogLadderResonance,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqMoogLadderResonance = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_moog_drive),
                    value = snapshot.eqMoogLadderDriveDb,
                    valueRange = 0f..18f,
                    steps = 179,
                    format = { "%.1f dB".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqMoogLadderDriveDb = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_moog_mix),
                    value = snapshot.eqMoogLadderMix,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqMoogLadderMix = it)) }
                )
            }
        }

        // ----------------------------------------------------------- 输出处理
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.VolumeUp, title = stringResource(R.string.sound_engine_output_processing))
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_compressor),
                description = stringResource(R.string.sound_engine_compressor_summary),
                checked = snapshot.eqCompressorEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqCompressorEnabled = it)) }
            )
            if (snapshot.eqCompressorEnabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_compressor_threshold),
                    value = snapshot.eqCompressorThresholdDb,
                    valueRange = -60f..0f,
                    format = { "%.1f dB".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqCompressorThresholdDb = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_compressor_ratio),
                    value = snapshot.eqCompressorRatio,
                    valueRange = 1f..12f,
                    format = { "%.1f:1".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqCompressorRatio = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_compressor_makeup),
                    value = snapshot.eqCompressorMakeupDb,
                    valueRange = -12f..12f,
                    steps = 239,
                    format = { "%+.1f dB".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqCompressorMakeupDb = it)) }
                )
            }
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_peak_limiter),
                description = stringResource(R.string.sound_engine_peak_limiter_summary),
                checked = snapshot.eqPeakLimiterEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqPeakLimiterEnabled = it)) }
            )
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_speaker_group),
                description = stringResource(R.string.sound_engine_speaker_group_summary),
                checked = snapshot.eqSpeakerOutputEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqSpeakerOutputEnabled = it)) }
            )
            if (snapshot.eqSpeakerOutputEnabled) {
                val spkModes = listOf(
                    0 to R.string.sound_engine_speaker_elasticity,
                    1 to R.string.sound_engine_speaker_powerful,
                    2 to R.string.sound_engine_speaker_wide
                )
                SoundChoiceItem(
                    title = stringResource(R.string.sound_engine_speaker_mode),
                    options = spkModes.map { it.first to stringResource(it.second) },
                    selected = snapshot.eqSpeakerOutputMode,
                    onSelect = { value -> onSnapshotChange(snapshot.copy(eqSpeakerOutputMode = value)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_speaker_strength),
                    value = snapshot.eqSpeakerOutputStrength,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqSpeakerOutputStrength = it)) }
                )
            }
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_headphone_group),
                description = stringResource(R.string.sound_engine_crossfeed_summary),
                checked = snapshot.eqCrossfeedEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqCrossfeedEnabled = it)) }
            )
            if (snapshot.eqCrossfeedEnabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_crossfeed_low_cut),
                    value = snapshot.eqCrossfeedLowCutHz,
                    valueRange = 50f..1000f,
                    steps = 189,
                    format = { "%.0f Hz".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqCrossfeedLowCutHz = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_crossfeed_high_cut),
                    value = snapshot.eqCrossfeedHighCutHz,
                    valueRange = 500f..8000f,
                    steps = 149,
                    format = { "%.0f Hz".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqCrossfeedHighCutHz = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_crossfeed_attenuation),
                    value = snapshot.eqCrossfeedAttenuationDb,
                    valueRange = 0f..15f,
                    steps = 149,
                    format = { "-%.1f dB".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqCrossfeedAttenuationDb = it)) }
                )
            }
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_mono_bass),
                description = stringResource(R.string.sound_engine_mono_bass_summary),
                checked = snapshot.eqMonoBassEnabled,
                onCheckedChange = { onSnapshotChange(snapshot.copy(eqMonoBassEnabled = it)) }
            )
            if (snapshot.eqMonoBassEnabled) {
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_mono_bass_crossover),
                    value = snapshot.eqMonoBassCrossoverHz,
                    valueRange = 60f..300f,
                    steps = 239,
                    format = { "%.0f Hz".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqMonoBassCrossoverHz = it)) }
                )
                SoundSliderRow(
                    title = stringResource(R.string.sound_engine_mono_bass_amount),
                    value = snapshot.eqMonoBassAmount,
                    valueRange = 0f..100f,
                    steps = 99,
                    format = { "%.0f%%".format(it) },
                    onValueChange = { onSnapshotChange(snapshot.copy(eqMonoBassAmount = it)) }
                )
            }
        }

        // ----------------------------------------------------------- 输出格式
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.NotificationsActive, title = stringResource(R.string.sound_engine_output_format))
            val bitDepths = listOf(
                HACHIMI_BIT_DEPTH_AUTO to R.string.sound_engine_bit_depth_auto,
                HACHIMI_BIT_DEPTH_16 to R.string.sound_engine_bit_depth_16,
                HACHIMI_BIT_DEPTH_24 to R.string.sound_engine_bit_depth_24,
                HACHIMI_BIT_DEPTH_32 to R.string.sound_engine_bit_depth_32,
                HACHIMI_BIT_DEPTH_FLOAT32 to R.string.sound_engine_bit_depth_float
            )
            SoundChoiceItem(
                title = stringResource(R.string.sound_engine_bit_depth),
                options = bitDepths.map { it.first to stringResource(it.second) },
                selected = snapshot.obBitDepth,
                onSelect = { value -> onSnapshotChange(snapshot.copy(obBitDepth = value)) }
            )
            val rates = listOf(
                0 to R.string.sound_engine_rate_follow,
                44100 to R.string.sound_engine_rate_44100,
                48000 to R.string.sound_engine_rate_48000,
                88200 to R.string.sound_engine_rate_88200,
                96000 to R.string.sound_engine_rate_96000,
                192000 to R.string.sound_engine_rate_192000
            )
            SoundChoiceItem(
                title = stringResource(R.string.sound_engine_sample_rate),
                options = rates.map { it.first to stringResource(it.second) },
                selected = snapshot.obSampleRate,
                onSelect = { value -> onSnapshotChange(snapshot.copy(obSampleRate = value)) }
            )
        }

        // ----------------------------------------------------------- 输出通道 (Oboe)
        MiuixSettingsSectionCard {
            SettingsSectionTitle(icon = Icons.Outlined.Usb, title = stringResource(R.string.sound_engine_backend_group))
            val backends = listOf(
                "auto" to R.string.sound_engine_backend_auto,
                "track" to R.string.sound_engine_backend_track,
                "aaudio" to R.string.sound_engine_backend_aaudio,
                "opensles" to R.string.sound_engine_backend_opensles
            )
            SoundChoiceItem(
                title = stringResource(R.string.sound_engine_backend),
                options = backends.map { it.first to stringResource(it.second) },
                selected = snapshot.obBackend,
                onSelect = { value -> onSnapshotChange(snapshot.copy(obBackend = value)) }
            )
            SettingsDivider()
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_usb_exclusive),
                description = stringResource(R.string.sound_engine_usb_exclusive_summary),
                checked = snapshot.obUsbExclusive,
                onCheckedChange = { onSnapshotChange(snapshot.copy(obUsbExclusive = it)) }
            )
            SoundSwitchItem(
                title = stringResource(R.string.sound_engine_usb_pinned),
                description = stringResource(R.string.sound_engine_usb_pinned_summary),
                checked = snapshot.obUsbPinned,
                onCheckedChange = { onSnapshotChange(snapshot.copy(obUsbPinned = it)) }
            )
        }
    }
}

// --------------------------------------------------------------------------- 行原语

@Composable
private fun SoundSwitchItem(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        trailingContent = {
            MiuixSettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun SoundSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String = { "%.2f".format(it) },
    valueText: String? = null,
    onValueChange: (Float) -> Unit
) {
    // 持久化值可能落在新范围之外（历史版本量纲不同），这里兜底钳制，避免 Slider 越界。
    val safeValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText ?: format(safeValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MiuixSettingsSlider(
            value = safeValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun <T> SoundChoiceItem(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    var dialogVisible by remember { mutableStateOf(false) }
    val selectedLabel = options
        .firstOrNull { it.first == selected }?.second
        ?: options.firstOrNull()?.second.orEmpty()

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .settingsItemClickable { dialogVisible = true },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    if (dialogVisible) {
        SoundChoiceDialog(
            title = title,
            options = options,
            selected = selected,
            onSelect = {
                dialogVisible = false
                onSelect(it)
            },
            onDismiss = { dialogVisible = false }
        )
    }
}

@Composable
private fun <T> SoundChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    MiuixSettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(options) { option ->
                    MiuixSettingsChoiceRow(
                        title = option.second,
                        selected = option.first == selected,
                        onClick = { onSelect(option.first) }
                    )
                }
            }
        },
        confirmButton = {
            MiuixSettingsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
