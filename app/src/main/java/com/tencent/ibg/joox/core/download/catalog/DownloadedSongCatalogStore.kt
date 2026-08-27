package com.tencent.ibg.joox.core.download.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.tencent.ibg.joox.core.download.DownloadedSong
import com.tencent.ibg.joox.core.logging.NPLogger
import com.tencent.ibg.joox.data.local.database.NeriUserDataDatabase

internal class DownloadedSongCatalogStore(
    private val cacheFileName: String,
    private val snapshotCacheKeyProvider: (Context) -> String,
    private val loggerTag: String
) {
    fun restore(context: Context): List<DownloadedSong>? {
        return runCatching {
            runBlocking(Dispatchers.IO) {
                roomStore(context).restore()
            }
        }.onFailure {
            NPLogger.w(loggerTag, "读取下载歌曲目录失败: ${it.message}")
        }.getOrNull()
    }

    fun persist(context: Context, songs: List<DownloadedSong>): Boolean {
        return runCatching {
            runBlocking(Dispatchers.IO) {
                persistDownloadedSongCatalogWithFallback(
                    store = roomStore(context),
                    songs = songs,
                    onRoomFailure = { error ->
                        NPLogger.e(
                            loggerTag,
                            "写入 Room 下载歌曲目录失败，降级写旧 JSON",
                            error
                        )
                    }
                )
            }
        }.onFailure {
            NPLogger.e(loggerTag, "写入下载歌曲目录失败", it)
        }.isSuccess
    }

    private fun roomStore(context: Context): DownloadedSongCatalogRoomStore {
        val appContext = context.applicationContext
        return DownloadedSongCatalogRoomStore(
            context = appContext,
            database = NeriUserDataDatabase.getInstance(appContext),
            cacheFileName = cacheFileName,
            snapshotCacheKeyProvider = snapshotCacheKeyProvider,
            loggerTag = loggerTag
        )
    }
}

internal enum class DownloadedSongCatalogPersistTarget {
    ROOM,
    LEGACY_JSON
}

internal interface DownloadedSongCatalogPersistenceStore {
    suspend fun persistCatalog(songs: List<DownloadedSong>)

    suspend fun persistLegacyFallback(songs: List<DownloadedSong>)
}

internal suspend fun persistDownloadedSongCatalogWithFallback(
    store: DownloadedSongCatalogPersistenceStore,
    songs: List<DownloadedSong>,
    onRoomFailure: (Throwable) -> Unit = {}
): DownloadedSongCatalogPersistTarget {
    return runCatching {
        store.persistCatalog(songs)
        DownloadedSongCatalogPersistTarget.ROOM
    }.getOrElse { error ->
        onRoomFailure(error)
        store.persistLegacyFallback(songs)
        DownloadedSongCatalogPersistTarget.LEGACY_JSON
    }
}
