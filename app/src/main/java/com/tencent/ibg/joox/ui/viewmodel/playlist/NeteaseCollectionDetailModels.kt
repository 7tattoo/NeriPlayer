package com.tencent.ibg.joox.ui.viewmodel.playlist

import com.tencent.ibg.joox.data.model.SongItem

data class NeteaseCollectionHeader(
    val id: Long,
    val isAlbum: Boolean,
    val name: String,
    val coverUrl: String,
    val playCount: Long,
    val trackCount: Int
)

data class NeteaseCollectionDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val header: NeteaseCollectionHeader? = null,
    val tracks: List<SongItem> = emptyList()
)
