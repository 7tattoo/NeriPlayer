package com.tencent.ibg.joox.core.startup.safemode

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tencent.ibg.joox.util.crash.CrashReportStore
import java.io.File

internal class SafeModeCrashReports(
    context: Context
) {
    private val appContext = context.applicationContext

    companion object {
        suspend fun readFullReport(reportFile: File): String? {
            return withContext(Dispatchers.IO) {
                CrashReportStore.readFullCrashReport(reportFile)
            }
        }
    }

    fun hasPendingReport(): Boolean {
        return CrashReportStore.hasPendingCrashReport(appContext)
    }

    suspend fun readPendingReport(): CrashReportStore.PendingCrashReport? {
        return withContext(Dispatchers.IO) {
            CrashReportStore.readPendingCrashReport(appContext)
        }
    }

    suspend fun readFullReport(reportFile: File): String? {
        return SafeModeCrashReports.readFullReport(reportFile)
    }

    suspend fun exportReport(reportFile: File, destination: Uri) {
        withContext(Dispatchers.IO) {
            CrashReportStore.exportCrashReport(
                context = appContext,
                reportFile = reportFile,
                destination = destination
            )
        }
    }

    fun clearPendingReport() {
        CrashReportStore.clearPendingCrashReport(appContext)
    }
}
