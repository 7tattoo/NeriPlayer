package com.tencent.ibg.joox.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NeteaseArtistSummary(
    val id: Long,
    val name: String
) : Parcelable
