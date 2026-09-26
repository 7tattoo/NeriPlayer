package com.tencent.ibg.joox.core.player.usb.device

import com.tencent.ibg.joox.core.player.model.PlaybackSoundConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbExclusiveCompatibilityPolicyTest {

    @Test
    fun `default sound configuration keeps native path eligible`() {
        assertFalse(PlaybackSoundConfig().requiresSystemAudioProcessor())
    }

    @Test
    fun `speed pitch and listen together correction require system processor`() {
        assertTrue(PlaybackSoundConfig(speed = 1.05f).requiresSystemAudioProcessor())
        assertTrue(PlaybackSoundConfig(pitch = 0.95f).requiresSystemAudioProcessor())
        assertTrue(
            PlaybackSoundConfig().requiresSystemAudioProcessor(listenTogetherSyncRate = 1.01f)
        )
    }

    @Test
    fun `platform audio effects require system audio session`() {
        assertTrue(PlaybackSoundConfig(equalizerEnabled = true).requiresSystemAudioProcessor())
    }

    @Test
    fun `imperceptible playback parameter noise keeps native path eligible`() {
        assertFalse(PlaybackSoundConfig(speed = 1.00005f).requiresSystemAudioProcessor())
        assertFalse(PlaybackSoundConfig(pitch = 0.99995f).requiresSystemAudioProcessor())
        assertFalse(
            PlaybackSoundConfig().requiresSystemAudioProcessor(listenTogetherSyncRate = 1.00005f)
        )
    }

    @Test
    fun `playback parameter changes beyond epsilon require system processor`() {
        assertTrue(PlaybackSoundConfig(speed = 1.0002f).requiresSystemAudioProcessor())
        assertTrue(PlaybackSoundConfig(pitch = 0.9998f).requiresSystemAudioProcessor())
        assertTrue(
            PlaybackSoundConfig().requiresSystemAudioProcessor(listenTogetherSyncRate = 1.0002f)
        )
    }
}
