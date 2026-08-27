package com.tencent.ibg.joox.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tencent.ibg.joox.data.local.playlist.model.LocalPlaylist
import com.tencent.ibg.joox.ui.component.playlist.playlistExportListHeight as newPlaylistExportListHeight

@Composable
internal fun PlaylistExportSheet(
    title: String,
    playlists: List<LocalPlaylist>,
    selectedCount: Int,
    onDismissRequest: () -> Unit,
    onCreateAndExport: (String) -> Unit,
    onExportToPlaylist: (LocalPlaylist) -> Unit,
    createActionLabel: String? = null
) {
    com.tencent.ibg.joox.ui.component.playlist.PlaylistExportSheet(
        title = title,
        playlists = playlists,
        selectedCount = selectedCount,
        onDismissRequest = onDismissRequest,
        onCreateAndExport = onCreateAndExport,
        onExportToPlaylist = onExportToPlaylist,
        createActionLabel = createActionLabel
    )
}

internal fun Modifier.playlistExportListHeight(): Modifier =
    newPlaylistExportListHeight()
