package com.tencent.ibg.joox.listentogether.validation

import android.content.Context
import androidx.annotation.StringRes
import com.tencent.ibg.joox.core.di.AppContainer
import com.tencent.ibg.joox.util.platform.LanguageManager

data class ListenTogetherValidationError(
    @param:StringRes val messageResId: Int,
    val args: List<Any> = emptyList()
) {
    fun format(context: Context): String {
        val localizedContext = LanguageManager.applyLanguage(context.applicationContext)
        return localizedContext.getString(messageResId, *args.toTypedArray())
    }

    fun formatForApp(): String {
        return format(AppContainer.applicationContext)
    }
}
