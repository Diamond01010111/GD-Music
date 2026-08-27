package com.diamond.gdapplication.data

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NeteasePlaylist(
    val id: String,
    val name: String,
    val coverUrl: String,
    val trackCount: Int,
    val creatorId: String
) {
    fun isCreatedBy(userId: String): Boolean = creatorId == userId
}

class NeteasePlaylistRepository {
    private val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadPublicPlaylists(
        userId: String,
        callback: (Result<List<NeteasePlaylist>>) -> Unit
    ) {
        loadPage(
            userId = userId,
            offset = 0,
            playlists = mutableListOf(),
            callback = callback
        )
    }

    private fun loadPage(
        userId: String,
        offset: Int,
        playlists: MutableList<NeteasePlaylist>,
        callback: (Result<List<NeteasePlaylist>>) -> Unit
    ) {
        val url = USER_PLAYLIST_URL.toHttpUrl().newBuilder()
            .addQueryParameter("uid", userId)
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("offset", offset.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Referer", "https://music.163.com/")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deliver(callback, Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            throw IOException("网易云请求失败：HTTP ${it.code}")
                        }

                        val root = JSONObject(body)
                        val code = root.optInt("code", it.code)
                        if (code != 200) {
                            val message = root.optString("message", "接口返回错误")
                            throw IOException("网易云请求失败：$message（$code）")
                        }

                        val array = root.optJSONArray("playlist")
                            ?: throw IOException("网易云没有返回歌单数据")

                        for (index in 0 until array.length()) {
                            val item = array.optJSONObject(index) ?: continue
                            val creator = item.optJSONObject("creator")
                            val playlistId = item.opt("id")?.toString().orEmpty()

                            if (playlistId.isBlank()) continue

                            playlists += NeteasePlaylist(
                                id = playlistId,
                                name = item.optString("name", "未命名歌单"),
                                coverUrl = item.optString("coverImgUrl")
                                    .replace("http://", "https://"),
                                trackCount = item.optInt("trackCount", 0),
                                creatorId = creator?.opt("userId")?.toString().orEmpty()
                            )
                        }

                        val hasMore = root.optBoolean("more", false)
                        if (hasMore && array.length() > 0) {
                            loadPage(
                                userId = userId,
                                offset = offset + array.length(),
                                playlists = playlists,
                                callback = callback
                            )
                        } else {
                            deliver(
                                callback,
                                Result.success(playlists.distinctBy { playlist -> playlist.id })
                            )
                        }
                    }
                } catch (e: Exception) {
                    deliver(callback, Result.failure(e))
                }
            }
        })
    }

    private fun deliver(
        callback: (Result<List<NeteasePlaylist>>) -> Unit,
        result: Result<List<NeteasePlaylist>>
    ) {
        mainHandler.post {
            callback(result)
        }
    }

    private companion object {
        const val USER_PLAYLIST_URL = "https://music.163.com/api/user/playlist/"
        const val PAGE_SIZE = 100
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
