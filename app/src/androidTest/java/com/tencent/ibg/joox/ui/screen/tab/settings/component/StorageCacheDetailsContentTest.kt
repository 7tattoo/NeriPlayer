package com.tencent.ibg.joox.ui.screen.tab.settings.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tencent.ibg.joox.R
import com.tencent.ibg.joox.data.storage.StorageUsageItem
import com.tencent.ibg.joox.data.storage.StorageUsageItemKind
import com.tencent.ibg.joox.data.storage.StorageUsageSection
import com.tencent.ibg.joox.data.storage.StorageUsageSummary
import com.tencent.ibg.joox.testutil.assumeComposeHostAvailable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageCacheDetailsContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun assumeDeviceUnlocked() {
        assumeComposeHostAvailable()
    }

    @Test
    fun scanningStateShowsDedicatedScanContent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            MaterialTheme {
                StorageCacheDetailsContent(
                    storageDetails = StorageUsageSummary.Empty,
                    isScanning = true,
                    onRefresh = {},
                    onClearCache = {},
                    onOpenSystemSettings = {}
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.storage_scan_title)
        ).assertExists()
    }

    @Test
    fun usageRowsDoNotRenderAbsolutePaths() {
        val summary = StorageUsageSummary(
            sections = listOf(
                StorageUsageSection(
                    title = "Cache",
                    items = listOf(
                        StorageUsageItem(
                            title = "Audio cache",
                            description = "Regenerable audio files",
                            path = "/private/cache/audio",
                            sizeBytes = 1L,
                            fileCount = 1,
                            kind = StorageUsageItemKind.AudioCache
                        )
                    )
                )
            )
        )
        composeRule.setContent {
            MaterialTheme {
                StorageCacheDetailsContent(
                    storageDetails = summary,
                    isScanning = false,
                    onRefresh = {},
                    onClearCache = {},
                    onOpenSystemSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("/private/cache/audio").assertDoesNotExist()
        composeRule.onNodeWithText("Audio cache").assertExists()
    }
}
