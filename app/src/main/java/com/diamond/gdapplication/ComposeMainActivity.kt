package com.diamond.gdapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.ui.MusicApp

class ComposeMainActivity : ComponentActivity() {

    private lateinit var api: GdMusicApi
    private lateinit var playerManager: PlayerManager
    private lateinit var musicController: MusicController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        api = GdMusicApi()
        playerManager = PlayerManager(this)
        musicController = MusicController(
            api,
            playerManager
        )

        setContent {
            MaterialTheme {
                var playerStatus by remember {
                    mutableStateOf("暂未播放")
                }

                DisposableEffect(Unit) {
                    musicController.setListener(
                        object : MusicController.Listener {

                            override fun onStatusChanged(message: String) {
                                playerStatus = message
                            }

                            override fun onStatusAppend(message: String) {
                                playerStatus += message
                            }

                            override fun onModeChanged(modeName: String) {
                                // 后续可以显示播放模式。
                            }
                        }
                    )

                    onDispose {
                        musicController.setListener(null)
                    }
                }

                MusicApp(
                    playerStatus = playerStatus,

                    onRequestSearch = { keyword, category, source, callback ->
                        requestTracks(
                            keyword = keyword,
                            category = category,
                            source = source,
                            callback = callback
                        )
                    },

                    onPlayResults = { tracks, index ->
                        musicController.setPlaylistAndPlay(
                            tracks,
                            index
                        )
                    },

                    onRecommendedSongClick = { keyword ->
                        musicController.searchAndPlayFirst(keyword)
                    },

                    onPlayPause = {
                        musicController.playOrPause()
                    }
                )
            }
        }
    }

    private fun requestTracks(
        keyword: String,
        category: SearchCategory,
        source: String,
        callback: (Result<List<Track>>) -> Unit
    ) {
        if (category == SearchCategory.NETEASE_PLAYLIST) {
            callback(
                Result.failure(
                    IllegalStateException(
                        "GD 音乐台 API 暂不支持网易云歌单搜索，请使用底部“网易歌单”页面粘贴分享链接。"
                    )
                )
            )
            return
        }

        val requestSource = when (category) {
            SearchCategory.SONG -> source
            SearchCategory.ALBUM -> "${source}_album"
            SearchCategory.NETEASE_PLAYLIST -> "netease"
        }

        api.searchTracks(
            keyword,
            requestSource,
            30,
            1,
            object : GdMusicApi.SearchCallback {

                override fun onSuccess(tracks: List<Track>) {
                    // 专辑接口使用 source_album，播放时恢复成基础音乐源。
                    tracks.forEach { track ->
                        track.source = source
                    }

                    runOnUiThread {
                        callback(
                            Result.success(tracks)
                        )
                    }
                }

                override fun onError(e: Exception) {
                    runOnUiThread {
                        callback(
                            Result.failure(e)
                        )
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::playerManager.isInitialized) {
            playerManager.release()
        }
    }
}