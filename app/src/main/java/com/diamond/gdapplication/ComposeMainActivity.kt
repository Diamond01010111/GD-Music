package com.diamond.gdapplication

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.ui.MusicApp
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay

@UnstableApi
class ComposeMainActivity : ComponentActivity() {

    private lateinit var api: GdMusicApi
    private lateinit var localPlaylistStore: LocalPlaylistStore
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController by mutableStateOf<MediaController?>(null)
    private var lastRootBackAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        api = GdMusicApi()
        localPlaylistStore = LocalPlaylistStore(this)
        requestNotificationPermission()
        connectToPlaybackService()

        setContent {
            MaterialTheme {
                val controller = mediaController
                var currentTrack by remember { mutableStateOf<Track?>(null) }
                var artworkUrl by remember { mutableStateOf("") }
                var isPlaying by remember { mutableStateOf(false) }
                var playMode by remember { mutableStateOf(PlaybackMode.LIST_LOOP) }
                var queue by remember { mutableStateOf<List<Track>>(emptyList()) }
                var currentIndex by remember { mutableStateOf(-1) }
                var playbackProgress by remember { mutableStateOf(0f) }
                var localPlaylists by remember {
                    mutableStateOf(localPlaylistStore.playlists)
                }

                fun syncPlayerState() {
                    if (controller == null) {
                        currentTrack = null
                        artworkUrl = ""
                        isPlaying = false
                        queue = emptyList()
                        currentIndex = -1
                        playbackProgress = 0f
                        return
                    }

                    currentTrack = TrackMediaItem.toTrack(controller.currentMediaItem)
                    artworkUrl = currentTrack?.picUrl.orEmpty()
                    isPlaying = controller.isPlaying
                    playMode = playbackMode(controller)
                    queue = (0 until controller.mediaItemCount).mapNotNull { index ->
                        TrackMediaItem.toTrack(controller.getMediaItemAt(index))
                    }
                    currentIndex = if (controller.mediaItemCount == 0) {
                        -1
                    } else {
                        controller.currentMediaItemIndex
                    }
                }

                LaunchedEffect(Unit) {
                    refreshMissingLocalArtwork {
                        localPlaylists = localPlaylistStore.playlists
                    }
                }

                DisposableEffect(controller) {
                    if (controller == null) {
                        onDispose { }
                    } else {
                        val listener = object : Player.Listener {
                            override fun onEvents(
                                player: Player,
                                events: Player.Events
                            ) {
                                syncPlayerState()
                            }
                        }
                        controller.addListener(listener)
                        syncPlayerState()
                        onDispose {
                            controller.removeListener(listener)
                        }
                    }
                }

                LaunchedEffect(controller) {
                    val connectedController = controller ?: return@LaunchedEffect
                    while (true) {
                        val duration = connectedController.duration
                        val position = connectedController.currentPosition
                        playbackProgress = if (duration > 0L && position >= 0L) {
                            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
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

                    onRequestSearch = { keyword, category, source, callback ->
                        requestTracks(keyword, category, source, callback)
                    },

                    onPlayResults = ::playTracks,

                    onRecommendedSongClick = ::searchAndPlayFirst,

                    onPlayPause = {
                        controllerOrWarn()?.let { player ->
                            if (player.isPlaying) player.pause() else player.play()
                        }
                    },

                    onSwitchPlayMode = ::switchPlaybackMode,

                    onSkipPrevious = {
                        controllerOrWarn()?.seekToPrevious()
                    },

                    onSkipNext = {
                        controllerOrWarn()?.seekToNext()
                    },

                    onQueueTrackClick = { index ->
                        controllerOrWarn()?.let { player ->
                            if (index in 0 until player.mediaItemCount) {
                                player.seekToDefaultPosition(index)
                                player.play()
                            }
                        }
                    },

                    onRemoveQueueTrack = { index ->
                        controllerOrWarn()?.let { player ->
                            if (index in 0 until player.mediaItemCount) {
                                player.removeMediaItem(index)
                                showToast("已从播放列表删除")
                            }
                        }
                    },

                    onClearQueue = {
                        controllerOrWarn()?.let { player ->
                            player.stop()
                            player.clearMediaItems()
                            showToast("播放列表已清空")
                        }
                    },

                    onPlayNext = { track ->
                        controllerOrWarn()?.let { player ->
                            val item = phoneMediaItem(track, player.mediaItemCount)
                            if (player.mediaItemCount == 0) {
                                player.setMediaItem(item)
                                player.prepare()
                                player.play()
                            } else {
                                val insertAt = (player.currentMediaItemIndex + 1)
                                    .coerceAtMost(player.mediaItemCount)
                                player.addMediaItem(insertAt, item)
                            }
                            showToast("下一首播放：${track.name}")
                        }
                    },

                    onAddToPlaylist = { track ->
                        controllerOrWarn()?.let { player ->
                            player.addMediaItem(phoneMediaItem(track, player.mediaItemCount))
                            showToast("已加入播放列表：${track.name}")
                        }
                    },

                    onCreateLocalPlaylist = { name, track ->
                        saveWithArtwork(track) { resolvedTrack ->
                            val playlist = localPlaylistStore.createPlaylist(name, resolvedTrack)
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
                        showToast(if (deleted) "收藏已删除" else "删除收藏失败")
                    },

                    onRemoveLocalPlaylistTrack = { playlistId, track ->
                        val removed = localPlaylistStore.removeTrackFromPlaylist(
                            playlistId,
                            track
                        )
                        localPlaylists = localPlaylistStore.playlists
                        showToast(
                            if (removed) "已移出当前收藏：${track.name}" else "移出收藏失败"
                        )
                    },

                    onRootBack = ::handleRootBack
                )
            }
        }
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(
            this,
            ComponentName(this, AutoPlaybackService::class.java)
        )
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    mediaController = future.get()
                } catch (error: Exception) {
                    showToast("播放器服务连接失败：${error.message.orEmpty()}")
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun playTracks(tracks: List<Track>, startIndex: Int) {
        val controller = controllerOrWarn() ?: return
        if (tracks.isEmpty() || startIndex !in tracks.indices) return

        val items = tracks.mapIndexed { index, track ->
            phoneMediaItem(track, index)
        }
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    private fun phoneMediaItem(track: Track, index: Int) = TrackMediaItem.create(
        "phone:${SystemClock.elapsedRealtime()}:$index:${track.source}:${track.id}",
        track
    )

    private fun searchAndPlayFirst(keyword: String) {
        api.searchTracks(
            keyword,
            object : GdMusicApi.SearchCallback {
                override fun onSuccess(tracks: List<Track>) {
                    runOnUiThread {
                        if (tracks.isEmpty()) {
                            showToast("没有搜索结果")
                        } else {
                            playTracks(tracks, 0)
                        }
                    }
                }

                override fun onError(e: Exception) {
                    runOnUiThread { showToast("搜索失败：${e.message.orEmpty()}") }
                }
            }
        )
    }

    private fun switchPlaybackMode() {
        val controller = controllerOrWarn() ?: return
        when (playbackMode(controller)) {
            PlaybackMode.LIST_LOOP -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ONE
                showToast("单曲循环")
            }
            PlaybackMode.SINGLE_LOOP -> {
                controller.repeatMode = Player.REPEAT_MODE_ALL
                controller.shuffleModeEnabled = true
                showToast("随机播放")
            }
            PlaybackMode.RANDOM -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ALL
                showToast("列表循环")
            }
        }
    }

    private fun playbackMode(player: Player): PlaybackMode = when {
        player.shuffleModeEnabled -> PlaybackMode.RANDOM
        player.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.SINGLE_LOOP
        else -> PlaybackMode.LIST_LOOP
    }

    private fun controllerOrWarn(): MediaController? {
        val controller = mediaController
        if (controller == null) {
            showToast("播放器正在连接，请稍后重试")
        }
        return controller
    }

    private fun refreshMissingLocalArtwork(onChanged: () -> Unit) {
        localPlaylistStore.playlists.forEach { playlist ->
            val coverTrack = playlist.coverTrack ?: return@forEach
            if (!coverTrack.picUrl.isNullOrBlank() && coverTrack.picUrl != "null") {
                return@forEach
            }

            api.getPicUrl(
                coverTrack,
                object : GdMusicApi.TrackCallback {
                    override fun onSuccess(updatedTrack: Track) {
                        runOnUiThread {
                            if (localPlaylistStore.updateTrackInPlaylist(
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

    private fun saveWithArtwork(track: Track, onReady: (Track) -> Unit) {
        if (!track.picUrl.isNullOrBlank()) {
            onReady(track)
            return
        }

        api.getPicUrl(
            track,
            object : GdMusicApi.TrackCallback {
                override fun onSuccess(updatedTrack: Track) {
                    runOnUiThread { onReady(updatedTrack) }
                }

                override fun onError(e: Exception) {
                    runOnUiThread { onReady(track) }
                }
            }
        )
    }

    private fun requestTracks(
        keyword: String,
        category: SearchCategory,
        source: String,
        callback: (Result<List<Track>>) -> Unit
    ) {
        if (keyword.isBlank()) {
            callback(Result.failure(IllegalArgumentException("搜索关键词不能为空")))
            return
        }
        if (category == SearchCategory.NETEASE_PLAYLIST) {
            callback(
                Result.failure(
                    IllegalStateException("GD 音乐台 API 暂不支持网易云歌单搜索")
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
                    tracks.forEach { it.source = source }
                    runOnUiThread { callback(Result.success(tracks)) }
                }

                override fun onError(e: Exception) {
                    runOnUiThread { callback(Result.failure(e)) }
                }
            }
        )
    }

    private fun handleRootBack() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRootBackAt <= EXIT_CONFIRM_WINDOW_MS) {
            finish()
            return
        }
        lastRootBackAt = now
        showToast("再按一次返回键退出")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        super.onDestroy()
    }

    private companion object {
        const val SEARCH_RESULT_COUNT = 30
        const val FIRST_PAGE = 1
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
        const val EXIT_CONFIRM_WINDOW_MS = 2_000L
    }
}
