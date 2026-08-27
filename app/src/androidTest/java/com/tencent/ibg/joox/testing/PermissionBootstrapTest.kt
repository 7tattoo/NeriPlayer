package com.tencent.ibg.joox.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tencent.ibg.joox.testutil.grantRuntimePermissions
import com.tencent.ibg.joox.testutil.playbackRuntimePermissions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionBootstrapTest {

    @Test
    fun grantPlaybackRuntimePermissions() {
        grantRuntimePermissions(*playbackRuntimePermissions())
    }
}
