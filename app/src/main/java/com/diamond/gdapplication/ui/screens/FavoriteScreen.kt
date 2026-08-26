package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.diamond.gdapplication.LocalPlaylistStore
import com.diamond.gdapplication.Track

@Composable
fun FavoriteScreen(
    playlists: List<LocalPlaylistStore.LocalPlaylist>,
    onPlayPlaylist: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onRemoveTrack: (String, Track) -> Unit
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    if (selectedPlaylist != null) {
        LocalPlaylistDetail(
            playlist = selectedPlaylist,
            onBack = { selectedPlaylistId = null },
            onPlayPlaylist = onPlayPlaylist,
            onPlayNext = onPlayNext,
            onAddToPlaylist = onAddToPlaylist,
            onFavorite = onFavorite,
            onRemoveTrack = { track -> onRemoveTrack(selectedPlaylist.id, track) }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "本地歌单",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 12.dp)
        )

        if (playlists.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "还没有本地歌单。收藏歌曲时可以创建一个。",
                    modifier = Modifier.padding(20.dp)
                )
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(playlists.size, key = { playlists[it].id }) { index ->
                val playlist = playlists[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedPlaylistId = playlist.id }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistCover(
                            track = playlist.coverTrack,
                            modifier = Modifier.size(72.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 14.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Text(
                                text = "${playlist.tracks.size} 首歌曲",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalPlaylistDetail(
    playlist: LocalPlaylistStore.LocalPlaylist,
    onBack: () -> Unit,
    onPlayPlaylist: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onRemoveTrack: (Track) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回本地歌单")
            }
            Text(text = playlist.name, style = MaterialTheme.typography.titleLarge)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCover(
                track = playlist.coverTrack,
                modifier = Modifier.size(112.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            ) {
                Text(text = playlist.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${playlist.tracks.size} 首歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Button(
                    enabled = playlist.tracks.isNotEmpty(),
                    onClick = { onPlayPlaylist(playlist.tracks, 0) },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("播放全部")
                }
            }
        }

        if (playlist.tracks.isEmpty()) {
            Text(text = "歌单中还没有歌曲", modifier = Modifier.padding(20.dp))
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(
                items = playlist.tracks,
                key = { index, track -> "${track.source}-${track.id}-$index" }
            ) { index, track ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPlayPlaylist(playlist.tracks, index) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistCover(track = track, modifier = Modifier.size(52.dp))
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        ) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        TrackMoreMenu(
                            track = track,
                            onPlayNext = onPlayNext,
                            onAddToPlaylist = onAddToPlaylist,
                            onFavorite = onFavorite,
                            onRemoveTrack = onRemoveTrack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackMoreMenu(
    track: Track,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onRemoveTrack: (Track) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("添加到下一首播放") },
                leadingIcon = {
                    Icon(Icons.Default.QueuePlayNext, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onPlayNext(track)
                }
            )
            DropdownMenuItem(
                text = { Text("加入播放列表") },
                leadingIcon = {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onAddToPlaylist(track)
                }
            )
            DropdownMenuItem(
                text = { Text("收藏") },
                leadingIcon = {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onFavorite(track)
                }
            )
            DropdownMenuItem(
                text = { Text("移出当前收藏") },
                leadingIcon = {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onRemoveTrack(track)
                }
            )
        }
    }
}

@Composable
private fun PlaylistCover(
    track: Track?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.aspectRatio(1f)) {
        val coverUrl = track?.picUrl.orEmpty()

        if (coverUrl.isNotBlank() && coverUrl != "null") {
            AsyncImage(
                model = coverUrl,
                contentDescription = track?.name ?: "歌单封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "默认歌单封面",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

