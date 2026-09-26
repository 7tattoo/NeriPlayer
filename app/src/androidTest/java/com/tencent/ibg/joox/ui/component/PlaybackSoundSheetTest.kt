package com.tencent.ibg.joox.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tencent.ibg.joox.R
import com.tencent.ibg.joox.core.player.model.PlaybackSoundState
import com.tencent.ibg.joox.testutil.assumeComposeHostAvailable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackSoundSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun assumeDeviceUnlocked() {
        assumeComposeHostAvailable()
    }

    @Test
    fun quickActions_dispatchExpectedCallbacks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val speedEvents = mutableListOf<Pair<Float, Boolean>>()
        val pitchEvents = mutableListOf<Pair<Float, Boolean>>()
        var resetCount = 0
        var dismissCount = 0

        composeRule.setContent {
            MaterialTheme {
                PlaybackSoundSheet(
                    state = PlaybackSoundState(),
                    onSpeedChange = { value, persist -> speedEvents += value to persist },
                    onPitchChange = { value, persist -> pitchEvents += value to persist },
                    onReset = { resetCount++ },
                    onDismiss = { dismissCount++ }
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(
                context.getString(R.string.nowplaying_audio_effects_title)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("3.00x").performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.nowplaying_audio_effects_reset)
        ).performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.action_done)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(3.0f to true), speedEvents)
            assertEquals(emptyList<Pair<Float, Boolean>>(), pitchEvents)
            assertEquals(1, resetCount)
            assertEquals(1, dismissCount)
        }
    }
}
