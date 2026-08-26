package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diamond.gdapplication.Track
import com.diamond.gdapplication.model.SearchCategory

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.QueuePlayNext

@Composable
fun SearchResultsScreen(
    keyword: String,
    category: SearchCategory,
    source: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回搜索"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 4.dp,
                        top = 6.dp
                    )
            ) {
                Text(
                    text = keyword,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "${category.label} · $source",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (tracks.isEmpty()) {
            Text(
                text = "没有找到搜索结果",
                modifier = Modifier.padding(20.dp)
            )

            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = tracks.size,
                key = { index ->
                    "${tracks[index].source}-${tracks[index].id}-$index"
                }
            ) { index ->
                val track = tracks[index]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onTrackClick(index)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )

                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )

                            if (track.album.isNotBlank()) {
                                Text(
                                    text = track.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }

                        Box {
                            var menuExpanded by remember {
                                mutableStateOf(false)
                            }

                            IconButton(
                                onClick = {
                                    menuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = {
                                    menuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("添加到下一首播放")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.QueuePlayNext,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onPlayNext(track)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("加入播放列表")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onAddToPlaylist(track)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("收藏到本地歌单")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onFavorite(track)
                                    }
                                )


                            }
                        }
                    }
                }
            }
        }
    }
}