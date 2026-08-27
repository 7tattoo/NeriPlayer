package com.tencent.ibg.joox.ui.component

import androidx.compose.runtime.Composable
import com.tencent.ibg.joox.data.model.SongItem

@Composable
fun LocalSongDetailsDialog(
    song: SongItem,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    com.tencent.ibg.joox.ui.component.local.LocalSongDetailsDialog(
        song = song,
        onDismiss = onDismiss,
        onShowMessage = onShowMessage
    )
}

@Composable
fun LocalSongSyncConfirmDialog(
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    com.tencent.ibg.joox.ui.component.local.LocalSongSyncConfirmDialog(
        actionLabel = actionLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
