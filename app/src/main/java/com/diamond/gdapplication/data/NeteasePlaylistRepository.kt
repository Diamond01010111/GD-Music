package com.diamond.gdapplication.data

import android.os.Handler
import android.os.Looper
import com.diamond.gdapplication.Track
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

    fun loadPlaylistTracks(
        playlistId: String,
        callback: (Result<List<Track>>) -> Unit
    ) {
        val modernUrl = PLAYLIST_DETAIL_URL.toHttpUrl().newBuilder()
            .addQueryParameter("id", playlistId)
            .addQueryParameter("n", MAX_PLAYLIST_TRACKS.toString())
            .addQueryParameter("s", "0")
            .build()

        loadTracksFromUrl(modernUrl.toString()) { modernResult ->
            modernResult.onSuccess { tracks ->
                callback(Result.success(tracks))
            }.onFailure {
                val legacyUrl = LEGACY_PLAYLIST_DETAIL_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("id", playlistId)
                    .build()
                loadTracksFromUrl(legacyUrl.toString(), callback)
            }
        }
    }

    private fun loadTracksFromUrl(
        url: String,
        callback: (Result<List<Track>>) -> Unit
    ) {
        execute(url) { root ->
            val playlist = root.optJSONObject("playlist")
                ?: root.optJSONObject("result")
                ?: throw IOException("网易云没有返回歌单详情")
            val array = playlist.optJSONArray("tracks")
                ?: throw IOException("网易云没有返回歌曲数据")
            if (array.length() == 0 && playlist.optInt("trackCount", 0) > 0) {
                throw IOException("网易云返回的歌曲数据不完整")
            }
            val tracks = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    parseTrack(item)?.let(::add)
                }
            }
            tracks
        }.onResult(callback)
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

    private fun <T> execute(
        url: String,
        transform: (JSONObject) -> T
    ): PendingResult<T> {
        return PendingResult { callback ->
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Referer", "https://music.163.com/")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    deliverResult(callback, Result.failure(e))
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
                            deliverResult(callback, Result.success(transform(root)))
                        }
                    } catch (e: Exception) {
                        deliverResult(callback, Result.failure(e))
                    }
                }
            })
        }
    }

    private fun parseTrack(item: JSONObject): Track? {
        val id = item.opt("id")?.toString().orEmpty()
        if (id.isBlank() || id == "null") return null

        val album = item.optJSONObject("al") ?: item.optJSONObject("album")
        val artists = item.optJSONArray("ar") ?: item.optJSONArray("artists")
        val artistNames = buildList {
            if (artists != null) {
                for (index in 0 until artists.length()) {
                    val name = artists.optJSONObject(index)?.optString("name").orEmpty()
                    if (name.isNotBlank()) add(name)
                }
            }
        }
        val picId = album?.opt("pic_str")?.toString()
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: album?.opt("picId")?.toString()
                ?.takeIf { it.isNotBlank() && it != "null" }
            ?: album?.opt("pic")?.toString().orEmpty()

        return Track(
            id,
            "netease",
            item.optString("name", "未知歌曲"),
            artistNames.joinToString("、"),
            album?.optString("name").orEmpty(),
            picId,
            id
        ).apply {
            picUrl = album?.optString("picUrl").orEmpty()
                .replace("http://", "https://")
            externalMetadata = true
        }
    }

    private fun deliver(
        callback: (Result<List<NeteasePlaylist>>) -> Unit,
        result: Result<List<NeteasePlaylist>>
    ) {
        mainHandler.post {
            callback(result)
        }
    }

    private fun <T> deliverResult(
        callback: (Result<T>) -> Unit,
        result: Result<T>
    ) {
        mainHandler.post { callback(result) }
    }

    private class PendingResult<T>(
        private val starter: ((Result<T>) -> Unit) -> Unit
    ) {
        fun onResult(callback: (Result<T>) -> Unit) {
            starter(callback)
        }
    }

    private companion object {
        const val USER_PLAYLIST_URL = "https://music.163.com/api/user/playlist/"
        const val PLAYLIST_DETAIL_URL = "https://music.163.com/api/v6/playlist/detail"
        const val LEGACY_PLAYLIST_DETAIL_URL = "https://music.163.com/api/playlist/detail"
        const val PAGE_SIZE = 100
        const val MAX_PLAYLIST_TRACKS = 100000
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
