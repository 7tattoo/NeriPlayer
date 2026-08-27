package com.tencent.ibg.joox.core.startup.logging

import android.content.Context
import com.tencent.ibg.joox.core.logging.NPLogger

internal object StartupLogInitializer {
    fun shouldEnableFileLogging(
        devModeEnabled: Boolean,
        alwaysRecordLogsEnabled: Boolean
    ): Boolean {
        return devModeEnabled || alwaysRecordLogsEnabled
    }

    fun sync(
        context: Context,
        devModeEnabled: Boolean,
        alwaysRecordLogsEnabled: Boolean
    ) {
        NPLogger.init(
            context = context,
            enableFileLogging = shouldEnableFileLogging(
                devModeEnabled = devModeEnabled,
                alwaysRecordLogsEnabled = alwaysRecordLogsEnabled
            )
        )
    }
}
