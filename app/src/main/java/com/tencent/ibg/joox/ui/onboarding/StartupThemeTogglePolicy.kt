package com.tencent.ibg.joox.ui.onboarding

import com.tencent.ibg.joox.data.settings.ThemeMode

internal fun shouldBlockStartupOnboardingThemeToggle(
    captureInFlight: Boolean,
    revealActive: Boolean
): Boolean = captureInFlight || revealActive

internal fun resolveStartupOnboardingThemeMode(
    storedFollowSystemDark: Boolean,
    storedForceDark: Boolean,
    pendingFollowSystemDark: Boolean?,
    pendingForceDark: Boolean?
): ThemeMode {
    return ThemeMode.fromPreferenceFlags(
        forceDark = pendingForceDark ?: storedForceDark,
        followSystemDark = pendingFollowSystemDark ?: storedFollowSystemDark
    )
}
