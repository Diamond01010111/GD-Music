package com.diamond.gdapplication.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.diamond.gdapplication.Track
import com.diamond.gdapplication.data.NeteasePlaylist
import com.diamond.gdapplication.data.NeteasePlaylistRepository
import com.diamond.gdapplication.ui.components.TrackMoreBottomSheet

@Composable
fun NeteasePlaylistScreen(
    onPlayPlaylist: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onSearchArtist: (String, String) -> Unit,
    onSearchAlbum: (String, String) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    val repository = remember { NeteasePlaylistRepository() }

    var savedUserId by rememberSaveable {
        mutableStateOf(preferences.getString(USER_ID_KEY, "").orEmpty())
    }
    var inputUserId by rememberSaveable { mutableStateOf(savedUserId) }
    var playlists by remember { mutableStateOf<List<NeteasePlaylist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestVersion by remember { mutableIntStateOf(0) }
    var selectedPlaylist by remember { mutableStateOf<NeteasePlaylist?>(null) }
    var selectedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingTracks by remember { mutableStateOf(false) }
    var trackError by remember { mutableStateOf<String?>(null) }
    var detailRequestVersion by remember { mutableIntStateOf(0) }

    fun refresh() {
        if (savedUserId.isBlank()) return

        val version = ++requestVersion
        isLoading = true
        errorMessage = null

        repository.loadPublicPlaylists(savedUserId) { result ->
            if (version != requestVersion) return@loadPublicPlaylists

            isLoading = false
            result.onSuccess { loaded ->
                playlists = loaded
            }.onFailure { error ->
                errorMessage = error.message ?: "刷新网易歌单失败"
            }
        }
    }

    fun loadTracks(playlist: NeteasePlaylist) {
        val version = ++detailRequestVersion
        isLoadingTracks = true
        trackError = null
        selectedTracks = emptyList()

        repository.loadPlaylistTracks(playlist.id) { result ->
            if (version != detailRequestVersion) return@loadPlaylistTracks

            isLoadingTracks = false
            result.onSuccess { tracks ->
                selectedTracks = tracks
            }.onFailure { error ->
                trackError = error.message ?: "加载歌单歌曲失败"
            }
        }
    }

    LaunchedEffect(savedUserId) {
        if (savedUserId.isNotBlank()) refresh()
    }

    LaunchedEffect(selectedPlaylist?.id) {
        selectedPlaylist?.let(::loadTracks)
    }

    if (savedUserId.isBlank()) {
        UserIdInput(
            userId = inputUserId,
            onUserIdChange = {
                inputUserId = it.filter(Char::isDigit)
                errorMessage = null
            },
            errorMessage = errorMessage,
            onConfirm = {
                val normalizedId = inputUserId.trim()
                if (normalizedId.isBlank()) {
                    errorMessage = "请输入网易云用户 ID"
                } else {
                    preferences.edit().putString(USER_ID_KEY, normalizedId).apply()
                    savedUserId = normalizedId
                }
            }
        )
        return
    }

    selectedPlaylist?.let { playlist ->
        PlaylistDetail(
            playlist = playlist,
            tracks = selectedTracks,
            isLoading = isLoadingTracks,
            errorMessage = trackError,
            onBack = {
                detailRequestVersion++
                selectedPlaylist = null
                selectedTracks = emptyList()
                trackError = null
            },
            onRetry = { loadTracks(playlist) },
            onPlayPlaylist = onPlayPlaylist,
            onPlayNext = onPlayNext,
            onAddToPlaylist = onAddToPlaylist,
            onFavorite = onFavorite,
            onSearchArtist = onSearchArtist,
            onSearchAlbum = onSearchAlbum
        )
        return
    }

    val createdPlaylists = playlists.filter { it.isCreatedBy(savedUserId) }
    val subscribedPlaylists = playlists.filterNot { it.isCreatedBy(savedUserId) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "网易歌单",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = PLAYLIST_GRID_CELL_SIZE),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && playlists.isEmpty()) {
                fullWidthItem {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            errorMessage?.let { message ->
                fullWidthItem {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (!isLoading && errorMessage == null && playlists.isEmpty()) {
                fullWidthItem {
                    Text(
                        text = "没有找到公开可见的歌单",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }

            if (createdPlaylists.isNotEmpty()) {
                sectionTitle("创建的歌单")
                items(createdPlaylists, key = { "created-${it.id}" }) { playlist ->
                    PlaylistCard(playlist, onClick = { selectedPlaylist = playlist })
                }
            }

            if (subscribedPlaylists.isNotEmpty()) {
                sectionTitle("收藏的歌单")
                items(subscribedPlaylists, key = { "subscribed-${it.id}" }) { playlist ->
                    PlaylistCard(playlist, onClick = { selectedPlaylist = playlist })
                }
            }

            fullWidthItem {
                PlaylistFooter(
                    userId = savedUserId,
                    isLoading = isLoading,
                    onRefresh = ::refresh,
                    onExit = {
                        requestVersion++
                        preferences.edit().remove(USER_ID_KEY).apply()
                        savedUserId = ""
                        inputUserId = ""
                        playlists = emptyList()
                        isLoading = false
                        errorMessage = null
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: NeteasePlaylist,
    tracks: List<Track>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayPlaylist: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onSearchArtist: (String, String) -> Unit,
    onSearchAlbum: (String, String) -> Unit
) {
    var moreTrack by remember { mutableStateOf<Track?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回网易歌单")
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistCover(
                        playlist = playlist,
                        modifier = Modifier.size(96.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${tracks.size} 首歌曲",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            enabled = tracks.isNotEmpty() && !isLoading,
                            onClick = { onPlayPlaylist(tracks, 0) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("播放全部")
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.padding(top = 10.dp)
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            } else if (tracks.isEmpty()) {
                item {
                    Text("歌单中没有可显示的歌曲", Modifier.padding(20.dp))
                }
            } else {
                itemsIndexed(
                    items = tracks,
                    key = { index, track -> "${track.id}-$index" }
                ) { index, track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlayPlaylist(tracks, index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist.ifBlank { "未知歌手" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { moreTrack = track }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                        }
                    }
                }
            }
        }
    }

    moreTrack?.let { track ->
        TrackMoreBottomSheet(
            track = track,
            onDismiss = { moreTrack = null },
            onPlayNext = onPlayNext,
            onAddToPlaylist = onAddToPlaylist,
            onFavorite = onFavorite,
            onSearchArtist = { artist -> onSearchArtist(artist, track.source) },
            onSearchAlbum = { album -> onSearchAlbum(album, track.source) }
        )
    }
}

@Composable
private fun UserIdInput(
    userId: String,
    onUserIdChange: (String) -> Unit,
    errorMessage: String?,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("网易歌单", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "输入网易云用户 ID，加载该用户公开创建和收藏的歌单。",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedTextField(
            value = userId,
            onValueChange = onUserIdChange,
            label = { Text("网易云用户 ID") },
            placeholder = { Text("例如：32953014") },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
        Button(
            enabled = userId.isNotBlank(),
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("保存并加载")
        }
        Text(
            text = "用户 ID 会保存在本机；重新打开此页面时会自动刷新。私密歌单不会显示。",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

private fun LazyGridScope.fullWidthItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.sectionTitle(title: String) {
    fullWidthItem {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: NeteasePlaylist,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.width(PLAYLIST_CARD_SIZE),
            onClick = onClick
        ) {
            Column {
                PlaylistCover(
                    playlist = playlist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
                )
                Text(
                    text = "${playlist.trackCount} 首",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistCover(
    playlist: NeteasePlaylist,
    modifier: Modifier = Modifier
) {
    if (playlist.coverUrl.isNotBlank()) {
        AsyncImage(
            model = playlist.coverUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LibraryMusic, contentDescription = "默认歌单封面")
        }
    }
}

@Composable
private fun PlaylistFooter(
    userId: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        HorizontalDivider()
        Text(
            text = "网易云用户 ID：$userId",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                enabled = !isLoading,
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isLoading) "刷新中" else "刷新")
            }
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f)
            ) {
                Text("退出")
            }
        }
    }
}

private val PLAYLIST_GRID_CELL_SIZE = 104.dp
private val PLAYLIST_CARD_SIZE = 96.dp
private const val PREFERENCES_NAME = "netease_playlist_preferences"
private const val USER_ID_KEY = "netease_user_id"
