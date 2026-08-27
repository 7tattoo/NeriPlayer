package com.tencent.ibg.joox.navigation

const val ACTION_LAUNCHER_SHORTCUT_CONTINUE_PLAYBACK =
    "com.tencent.ibg.joox.action.CONTINUE_PLAYBACK"
const val ACTION_LAUNCHER_SHORTCUT_EXPLORE =
    "com.tencent.ibg.joox.action.OPEN_EXPLORE"
const val ACTION_LAUNCHER_SHORTCUT_LIBRARY =
    "com.tencent.ibg.joox.action.OPEN_LIBRARY"
const val ACTION_LAUNCHER_SHORTCUT_SHUFFLE_FAVORITES =
    "com.tencent.ibg.joox.action.SHUFFLE_FAVORITES"

enum class LauncherShortcutAction {
    ContinuePlayback,
    OpenExplore,
    OpenLibrary,
    ShuffleFavorites
}

data class LauncherShortcutRequest(
    val token: Long,
    val action: LauncherShortcutAction
)

fun launcherShortcutActionFromIntentAction(action: String?): LauncherShortcutAction? {
    return when (action) {
        ACTION_LAUNCHER_SHORTCUT_CONTINUE_PLAYBACK ->
            LauncherShortcutAction.ContinuePlayback
        ACTION_LAUNCHER_SHORTCUT_EXPLORE ->
            LauncherShortcutAction.OpenExplore
        ACTION_LAUNCHER_SHORTCUT_LIBRARY ->
            LauncherShortcutAction.OpenLibrary
        ACTION_LAUNCHER_SHORTCUT_SHUFFLE_FAVORITES ->
            LauncherShortcutAction.ShuffleFavorites
        else -> null
    }
}

fun launcherShortcutMainTabRoute(action: LauncherShortcutAction): String? {
    return when (action) {
        LauncherShortcutAction.OpenExplore -> Destinations.Explore.route
        LauncherShortcutAction.OpenLibrary -> Destinations.Library.route
        LauncherShortcutAction.ContinuePlayback,
        LauncherShortcutAction.ShuffleFavorites -> null
    }
}
