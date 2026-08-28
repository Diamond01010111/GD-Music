package com.diamond.gdapplication.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NeteasePlaylistCache {
    private const val PREFERENCES_NAME = "netease_playlist_preferences"
    private const val USER_ID_KEY = "netease_user_id"
    private const val CACHED_USER_ID_KEY = "cached_user_id"
    private const val CACHED_PLAYLISTS_KEY = "cached_playlists"
    private const val LAST_REFRESH_ATTEMPT_KEY = "last_refresh_attempt"
    private const val REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1000L

    @JvmStatic
    fun savedUserId(context: Context): String = preferences(context)
        .getString(USER_ID_KEY, "")
        .orEmpty()

    @JvmStatic
    fun saveUserId(context: Context, userId: String) {
        preferences(context).edit().putString(USER_ID_KEY, userId).apply()
    }

    @JvmStatic
    fun read(context: Context, userId: String = savedUserId(context)): List<NeteasePlaylist> {
        val preferences = preferences(context)
        if (
            userId.isBlank() ||
            preferences.getString(CACHED_USER_ID_KEY, "") != userId
        ) {
            return emptyList()
        }

        return try {
            val array = JSONArray(preferences.getString(CACHED_PLAYLISTS_KEY, "[]"))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue

                    add(
                        NeteasePlaylist(
                            id = id,
                            name = item.optString("name", "未命名歌单"),
                            coverUrl = item.optString("coverUrl"),
                            trackCount = item.optInt("trackCount", 0),
                            creatorId = item.optString("creatorId")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @JvmStatic
    fun save(context: Context, userId: String, playlists: List<NeteasePlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("coverUrl", playlist.coverUrl)
                    .put("trackCount", playlist.trackCount)
                    .put("creatorId", playlist.creatorId)
            )
        }

        preferences(context).edit()
            .putString(CACHED_USER_ID_KEY, userId)
            .putString(CACHED_PLAYLISTS_KEY, array.toString())
            .apply()
    }

    @JvmStatic
    fun markRefreshAttempt(context: Context) {
        preferences(context).edit()
            .putLong(LAST_REFRESH_ATTEMPT_KEY, System.currentTimeMillis())
            .apply()
    }

    @JvmStatic
    fun shouldRefresh(context: Context): Boolean {
        val lastAttempt = preferences(context).getLong(LAST_REFRESH_ATTEMPT_KEY, 0L)
        return lastAttempt == 0L ||
            System.currentTimeMillis() - lastAttempt >= REFRESH_INTERVAL_MS
    }

    @JvmStatic
    fun clear(context: Context, clearUserId: Boolean = false) {
        val editor = preferences(context).edit()
            .remove(CACHED_USER_ID_KEY)
            .remove(CACHED_PLAYLISTS_KEY)
            .remove(LAST_REFRESH_ATTEMPT_KEY)
        if (clearUserId) editor.remove(USER_ID_KEY)
        editor.apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
}
