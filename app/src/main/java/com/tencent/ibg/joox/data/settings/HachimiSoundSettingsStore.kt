package com.tencent.ibg.joox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tencent.ibg.joox.core.logging.NPLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 音效引擎（Hachimi DSP）设置仓库。
 *
 * 持久化复用 [Context.dataStore]，整份快照序列化到一个 JSON 字符串键（与
 * [UsbExclusiveSettingsStore] 同一套 DataStore 写法）。字段与 DSP 入参一一对应，
 * 编解码由 [HachimiSoundSnapshot] 负责。
 */
internal class HachimiSoundSettingsStore(private val context: Context) {

    val settingsFlow: Flow<HachimiSoundSnapshot> =
        context.dataStore.data.map { preferences -> decode(preferences[KEY_SOUND_ENGINE]) }

    suspend fun currentSettings(): HachimiSoundSnapshot =
        decode(context.dataStore.data.first()[KEY_SOUND_ENGINE])

    suspend fun update(transform: (HachimiSoundSnapshot) -> HachimiSoundSnapshot) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENGINE] = transform(decode(preferences[KEY_SOUND_ENGINE])).toJson().toString()
        }
    }

    /** 整份覆盖（设置页批量保存时使用）。 */
    suspend fun setSnapshot(snapshot: HachimiSoundSnapshot) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENGINE] = snapshot.toJson().toString()
        }
    }

    private fun decode(raw: String?): HachimiSoundSnapshot {
        if (raw.isNullOrBlank()) return HachimiSoundSnapshot.DEFAULT
        return HachimiSoundSnapshot.fromJsonString(raw).also { snapshot ->
            if (snapshot === HachimiSoundSnapshot.DEFAULT && raw.length > 2) {
                // fromJsonString 内部已 runCatching 回退；此处仅留日志线索
                NPLogger.w(TAG, "音效设置 JSON 无法解析，已回退默认值 (len=${raw.length})")
            }
        }
    }

    private companion object {
        const val TAG = "HachimiSoundSettings"
        val KEY_SOUND_ENGINE = stringPreferencesKey("hachimi_sound_engine_json")
    }
}
