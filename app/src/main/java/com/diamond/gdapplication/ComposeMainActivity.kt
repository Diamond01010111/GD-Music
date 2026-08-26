package com.diamond.gdapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.ui.MusicApp
import kotlinx.coroutines.delay

class ComposeMainActivity : ComponentActivity() {

    private lateinit var api: GdMusicApi
    private lateinit var playerManager: PlayerManager
    private lateinit var musicController: MusicController
    private lateinit var localPlaylistStore: LocalPlaylistStore
    private lateinit var playbackNotificationManager: PlaybackNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        api = GdMusicApi()
        playerManager = PlayerManager(this)
        musicController = MusicController(api, playerManager)
        localPlaylistStore = LocalPlaylistStore(this)
        playbackNotificationManager = PlaybackNotificationManager(
            this,
            musicController,
            playerManager.player
        )
        requestNotificationPermission()

        setContent {
            MaterialTheme {
                var currentTrack by remember {
                    mutableStateOf<Track?>(null)
                }

                var artworkUrl by remember {
                    mutableStateOf("")
                }

                var isPlaying by remember {
                    mutableStateOf(false)
                }

                var playMode by remember {
                    mutableStateOf(MusicController.PlayMode.LIST_LOOP)
                }

                var queue by remember {
                    mutableStateOf<List<Track>>(emptyList())
                }

                var currentIndex by remember {
                    mutableStateOf(-1)
                }

                var playbackProgress by remember {
                    mutableStateOf(0f)
                }

                var localPlaylists by remember {
                    mutableStateOf(localPlaylistStore.playlists)
                }

                LaunchedEffect(Unit) {
                    refreshMissingLocalArtwork {
                        localPlaylists = localPlaylistStore.playlists
                    }
                }

                DisposableEffect(Unit) {
                    musicController.setListener(
                        object : MusicController.Listener {
                            override fun onStatusChanged(message: String) {
                            }

                            override fun onStatusAppend(message: String) {
                            }

                            override fun onModeChanged(modeName: String) {
                            }

                            override fun onTrackChanged(track: Track) {
                                currentTrack = track
                                artworkUrl = track.picUrl ?: ""
                                playbackNotificationManager.invalidate()
                            }

                            override fun onPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                                playbackNotificationManager.invalidate()
                            }

                            override fun onPlayModeChanged(
                                newMode: MusicController.PlayMode
                            ) {
                                playMode = newMode
                                playbackNotificationManager.invalidate()
                            }

                            override fun onQueueChanged(
                                tracks: List<Track>,
                                index: Int
                            ) {
                                queue = tracks
                                currentIndex = index
                            }

                            override fun onTrackCleared() {
                                currentTrack = null
                                artworkUrl = ""
                                isPlaying = false
                                playbackProgress = 0f
                                playbackNotificationManager.invalidate()
                            }
                        }
                    )

                    onDispose {
                        musicController.setListener(null)
                    }
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        val player = playerManager.getPlayer()
                        val duration = player.duration
                        val position = player.currentPosition

                        playbackProgress = if (
                            duration > 0L &&
                            position >= 0L
                        ) {
                            (
                                position.toFloat() /
                                    duration.toFloat()
                            ).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        delay(500)
                    }
                }

                MusicApp(
                    nowPlayingTrack = currentTrack,
                    artworkUrl = artworkUrl,
                    isPlaying = isPlaying,
                    playMode = playMode,
                    queue = queue,
                    currentIndex = currentIndex,
                    playbackProgress = playbackProgress,
                    localPlaylists = localPlaylists,

                    onRequestSearch = {
                            keyword,
                            category,
                            source,
                            callback ->

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
                    },

                    onSwitchPlayMode = {
                        musicController.switchPlayMode()
                    },

                    onQueueTrackClick = { index ->
                        musicController.playAtIndex(index)
                    },

                    onRemoveQueueTrack = { index ->
                        musicController.removeFromPlaylist(index)
                        showToast("已从播放列表删除")
                    },

                    onClearQueue = {
                        musicController.clearPlaylist()
                        showToast("播放列表已清空")
                    },

                    onPlayNext = { track ->
                        musicController.addToPlayNext(track)
                        showToast("下一首播放：${track.name}")
                    },

                    onAddToPlaylist = { track ->
                        musicController.addToPlaylist(track)
                        showToast("已加入播放列表：${track.name}")
                    },

                    onCreateLocalPlaylist = { name, track ->
                        saveWithArtwork(track) { resolvedTrack ->
                            val playlist = localPlaylistStore.createPlaylist(
                                name,
                                resolvedTrack
                            )
                            localPlaylists = localPlaylistStore.playlists

                            if (playlist != null) {
                                showToast("已创建收藏并添加：${track.name}")
                            } else {
                                showToast("收藏名称不能为空")
                            }
                        }
                    },

                    onCreateEmptyFavorite = { name ->
                        val favorite = localPlaylistStore.createPlaylist(name)
                        localPlaylists = localPlaylistStore.playlists

                        if (favorite != null) {
                            showToast("已创建收藏：${favorite.name}")
                        } else {
                            showToast("收藏名称不能为空")
                        }
                    },

                    onAddToLocalPlaylist = { playlistId, track ->
                        saveWithArtwork(track) { resolvedTrack ->
                            val added = localPlaylistStore.addTrackToPlaylist(
                                playlistId,
                                resolvedTrack
                            )
                            localPlaylists = localPlaylistStore.playlists

                            if (added) {
                                showToast("已收藏：${track.name}")
                            } else {
                                showToast("歌曲已在该收藏中")
                            }
                        }
                    },

                    onDeleteFavorite = { favoriteId ->
                        val deleted = localPlaylistStore.deletePlaylist(favoriteId)
                        localPlaylists = localPlaylistStore.playlists

                        if (deleted) {
                            showToast("收藏已删除")
                        } else {
                            showToast("删除收藏失败")
                        }
                    },

                    onRemoveLocalPlaylistTrack = { playlistId, track ->
                        val removed = localPlaylistStore.removeTrackFromPlaylist(
                            playlistId,
                            track
                        )
                        localPlaylists = localPlaylistStore.playlists

                        if (removed) {
                            showToast("已移出当前收藏：${track.name}")
                        } else {
                            showToast("移出收藏失败")
                        }
                    }
                )
            }
        }
    }

    private fun refreshMissingLocalArtwork(
        onChanged: () -> Unit
    ) {
        localPlaylistStore.playlists.forEach { playlist ->
            val coverTrack = playlist.coverTrack ?: return@forEach

            if (
                !coverTrack.picUrl.isNullOrBlank() &&
                coverTrack.picUrl != "null"
            ) {
                return@forEach
            }

            api.getPicUrl(
                coverTrack,
                object : GdMusicApi.TrackCallback {
                    override fun onSuccess(updatedTrack: Track) {
                        runOnUiThread {
                            if (
                                localPlaylistStore.updateTrackInPlaylist(
                                    playlist.id,
                                    updatedTrack
                                )
                            ) {
                                onChanged()
                            }
                        }
                    }

                    override fun onError(e: Exception) {
                        // 保留默认封面，下次启动时再次尝试。
                    }
                }
            )
        }
    }

    private fun saveWithArtwork(
        track: Track,
        onReady: (Track) -> Unit
    ) {
        if (!track.picUrl.isNullOrBlank()) {
            onReady(track)
            return
        }

        api.getPicUrl(
            track,
            object : GdMusicApi.TrackCallback {
                override fun onSuccess(updatedTrack: Track) {
                    runOnUiThread {
                        onReady(updatedTrack)
                    }
                }

                override fun onError(e: Exception) {
                    runOnUiThread {
                        onReady(track)
                    }
                }
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this@ComposeMainActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    private fun requestTracks(
        keyword: String,
        category: SearchCategory,
        source: String,
        callback: (Result<List<Track>>) -> Unit
    ) {
        if (keyword.isBlank()) {
            callback(
                Result.failure(
                    IllegalArgumentException("搜索关键词不能为空")
                )
            )
            return
        }

        if (category == SearchCategory.NETEASE_PLAYLIST) {
            callback(
                Result.failure(
                    IllegalStateException(
                        "GD 音乐台 API 暂不支持网易云歌单搜索"
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
            SEARCH_RESULT_COUNT,
            FIRST_PAGE,
            object : GdMusicApi.SearchCallback {
                override fun onSuccess(tracks: List<Track>) {
                    tracks.forEach { track ->
                        track.source = source
                    }

                    runOnUiThread {
                        callback(Result.success(tracks))
                    }
                }

                override fun onError(e: Exception) {
                    runOnUiThread {
                        callback(Result.failure(e))
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::musicController.isInitialized) {
            musicController.setListener(null)
        }

        if (::playbackNotificationManager.isInitialized) {
            playbackNotificationManager.release()
        }

        if (::playerManager.isInitialized) {
            playerManager.release()
        }
    }

    private companion object {
        const val SEARCH_RESULT_COUNT = 30
        const val FIRST_PAGE = 1
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }
}
