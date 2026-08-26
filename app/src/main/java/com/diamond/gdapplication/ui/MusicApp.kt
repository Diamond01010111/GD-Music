package com.diamond.gdapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diamond.gdapplication.LocalPlaylistStore
import com.diamond.gdapplication.MusicController
import com.diamond.gdapplication.Track
import com.diamond.gdapplication.model.AppPage
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.ui.components.MiniPlayer
import com.diamond.gdapplication.ui.components.QueueBottomSheet
import com.diamond.gdapplication.ui.screens.FavoriteScreen
import com.diamond.gdapplication.ui.screens.HomeScreen
import com.diamond.gdapplication.ui.screens.NeteasePlaylistScreen
import com.diamond.gdapplication.ui.screens.SearchResultsScreen
import com.diamond.gdapplication.ui.screens.SearchScreen

@Composable
fun MusicApp(
    nowPlayingTrack: Track?,
    artworkUrl: String,
    isPlaying: Boolean,
    playMode: MusicController.PlayMode,
    queue: List<Track>,
    currentIndex: Int,
    playbackProgress: Float,
    localPlaylists: List<LocalPlaylistStore.LocalPlaylist>,

    onRequestSearch: (
        keyword: String,
        category: SearchCategory,
        source: String,
        callback: (Result<List<Track>>) -> Unit
    ) -> Unit,

    onPlayResults: (
        tracks: List<Track>,
        index: Int
    ) -> Unit,

    onRecommendedSongClick: (String) -> Unit,
    onPlayPause: () -> Unit,
    onSwitchPlayMode: () -> Unit,
    onQueueTrackClick: (Int) -> Unit,
    onRemoveQueueTrack: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onCreateLocalPlaylist: (String, Track) -> Unit,
    onAddToLocalPlaylist: (String, Track) -> Unit,
    onPlayNext: (Track) -> Unit
) {
    var currentPage by remember {
        mutableStateOf(AppPage.HOME)
    }

    var resultKeyword by remember {
        mutableStateOf("")
    }

    var resultCategory by remember {
        mutableStateOf(SearchCategory.SONG)
    }

    var resultSource by remember {
        mutableStateOf("netease")
    }

    var resultTracks by remember {
        mutableStateOf<List<Track>>(emptyList())
    }

    var isSearching by remember {
        mutableStateOf(false)
    }

    var searchError by remember {
        mutableStateOf<String?>(null)
    }

    var showQueue by remember {
        mutableStateOf(false)
    }

    var pendingFavoriteTrack by remember {
        mutableStateOf<Track?>(null)
    }

    val showNavigationBar =
        currentPage == AppPage.HOME ||
                currentPage == AppPage.FAVORITE ||
                currentPage == AppPage.NETEASE_PLAYLIST

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,

        bottomBar = {
            Column {
                MiniPlayer(
                    track = nowPlayingTrack,
                    artworkUrl = artworkUrl,
                    isPlaying = isPlaying,
                    playMode = playMode,
                    playbackProgress = playbackProgress,
                    onPlayPause = onPlayPause,
                    onSwitchPlayMode = onSwitchPlayMode,
                    onOpenQueue = {
                        showQueue = true
                    }
                )

                if (showNavigationBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentPage == AppPage.HOME,
                            onClick = {
                                currentPage = AppPage.HOME
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "主页"
                                )
                            },
                            label = {
                                Text("主页")
                            }
                        )

                        NavigationBarItem(
                            selected = currentPage == AppPage.FAVORITE,
                            onClick = {
                                currentPage = AppPage.FAVORITE
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "收藏"
                                )
                            },
                            label = {
                                Text("收藏")
                            }
                        )

                        NavigationBarItem(
                            selected = currentPage == AppPage.NETEASE_PLAYLIST,
                            onClick = {
                                currentPage = AppPage.NETEASE_PLAYLIST
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "网易歌单"
                                )
                            },
                            label = {
                                Text("网易歌单")
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentPage) {
                AppPage.HOME -> {
                    HomeScreen(
                        onOpenSearch = {
                            searchError = null
                            currentPage = AppPage.SEARCH
                        },
                        onSongClick = onRecommendedSongClick
                    )
                }

                AppPage.FAVORITE -> {
                    FavoriteScreen(
                        playlists = localPlaylists,
                        onPlayPlaylist = onPlayResults
                    )
                }

                AppPage.NETEASE_PLAYLIST -> {
                    NeteasePlaylistScreen()
                }

                AppPage.SEARCH -> {
                    SearchScreen(
                        isSearching = isSearching,
                        errorMessage = searchError,
                        onBack = {
                            searchError = null
                            currentPage = AppPage.HOME
                        },
                        onSearch = {
                                keyword,
                                category,
                                source ->

                            isSearching = true
                            searchError = null

                            onRequestSearch(
                                keyword,
                                category,
                                source
                            ) { result ->
                                isSearching = false

                                result.onSuccess { tracks ->
                                    resultKeyword = keyword
                                    resultCategory = category
                                    resultSource = source
                                    resultTracks = tracks
                                    currentPage = AppPage.SEARCH_RESULTS
                                }

                                result.onFailure { error ->
                                    searchError = error.message ?: "搜索失败"
                                }
                            }
                        }
                    )
                }

                AppPage.SEARCH_RESULTS -> {
                    SearchResultsScreen(
                        keyword = resultKeyword,
                        category = resultCategory,
                        source = resultSource,
                        tracks = resultTracks,
                        onBack = {
                            currentPage = AppPage.SEARCH
                        },
                        onTrackClick = { index ->
                            onPlayResults(resultTracks, index)
                        },
                        onPlayNext = onPlayNext,
                        onAddToPlaylist = onAddToPlaylist,
                        onFavorite = { track ->
                            pendingFavoriteTrack = track
                        }
                    )
                }
            }
        }
    }

    if (showQueue) {
        QueueBottomSheet(
            tracks = queue,
            currentIndex = currentIndex,
            onDismiss = {
                showQueue = false
            },
            onTrackClick = { index ->
                onQueueTrackClick(index)
            },
            onRemoveTrack = { index ->
                onRemoveQueueTrack(index)
            },
            onClearQueue = {
                onClearQueue()
            }
        )
    }

    pendingFavoriteTrack?.let { track ->
        AddToLocalPlaylistDialog(
            track = track,
            playlists = localPlaylists,
            onDismiss = {
                pendingFavoriteTrack = null
            },
            onCreatePlaylist = { name ->
                onCreateLocalPlaylist(name, track)
                pendingFavoriteTrack = null
            },
            onSelectPlaylist = { playlistId ->
                onAddToLocalPlaylist(playlistId, track)
                pendingFavoriteTrack = null
            }
        )
    }
}

@Composable
private fun AddToLocalPlaylistDialog(
    track: Track,
    playlists: List<LocalPlaylistStore.LocalPlaylist>,
    onDismiss: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onSelectPlaylist: (String) -> Unit
) {
    var newPlaylistName by remember(track.id) {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("收藏到本地歌单")
        },
        text = {
            Column {
                Text(
                    text = track.name,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (playlists.isNotEmpty()) {
                    Text("选择已有歌单")

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .padding(top = 4.dp)
                    ) {
                        items(
                            items = playlists,
                            key = { it.id }
                        ) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectPlaylist(playlist.id)
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${playlist.tracks.size} 首")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = {
                        newPlaylistName = it
                    },
                    label = {
                        Text("新歌单名称")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newPlaylistName.isNotBlank(),
                onClick = {
                    onCreatePlaylist(newPlaylistName.trim())
                }
            ) {
                Text("创建并收藏")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
