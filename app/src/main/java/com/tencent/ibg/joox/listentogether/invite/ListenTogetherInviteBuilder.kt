package com.tencent.ibg.joox.listentogether.invite

import android.net.Uri
import com.tencent.ibg.joox.listentogether.network.http.normalizeBaseUrl
import com.tencent.ibg.joox.listentogether.validation.requireValidListenTogetherJoinSecret
import com.tencent.ibg.joox.listentogether.validation.requireValidListenTogetherNickname
import com.tencent.ibg.joox.listentogether.validation.requireValidListenTogetherRoomId
import java.util.UUID

private const val LISTEN_TOGETHER_INVITE_SCHEME = "neriplayer"
private const val LISTEN_TOGETHER_INVITE_HOST = "listen-together"
internal const val LISTEN_TOGETHER_INVITE_JOIN_PATH = "join"

fun buildListenTogetherUserUuid(): String {
    return UUID.randomUUID().toString()
}

fun buildDefaultListenTogetherNickname(): String {
    return "Neri${UUID.randomUUID().toString().replace("-", "").take(6).uppercase()}"
}

fun buildListenTogetherInviteUri(
    roomId: String,
    inviterNickname: String? = null,
    baseUrl: String? = null,
    joinSecret: String
): String {
    val normalizedRoomId = requireValidListenTogetherRoomId(roomId)
    val normalizedJoinSecret = requireValidListenTogetherJoinSecret(joinSecret)
    val normalizedBaseUrl = baseUrl
        ?.takeIf { it.isNotBlank() }
        ?.normalizeBaseUrl()
        ?.takeUnless { isDefaultListenTogetherBaseUrl(it) }
    return Uri.Builder()
        .scheme(LISTEN_TOGETHER_INVITE_SCHEME)
        .authority(LISTEN_TOGETHER_INVITE_HOST)
        .appendPath(LISTEN_TOGETHER_INVITE_JOIN_PATH)
        .appendQueryParameter("roomId", normalizedRoomId)
        .apply {
            inviterNickname?.takeIf { it.isNotBlank() }?.let {
                appendQueryParameter("inviter", requireValidListenTogetherNickname(it))
            }
            normalizedBaseUrl?.let {
                appendQueryParameter("baseUrl", it)
            }
            appendQueryParameter("secret", normalizedJoinSecret)
        }
        .build()
        .toString()
}
