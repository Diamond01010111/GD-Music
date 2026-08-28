package com.diamond.gdmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diamond.gdmusic.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    tracks: List<Track>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onClearQueue: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, tracks.size) {
        if (currentIndex in tracks.indices) {
            listState.scrollToItem(currentIndex)
        }
    }

    var showClearConfirmation by remember {
        mutableStateOf(false)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor =
            MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
        ) {
            QueueHeader(
                trackCount = tracks.size,
                onClearClick = {
                    showClearConfirmation = true
                },
                onDismiss = onDismiss
            )

            HorizontalDivider()

            if (tracks.isEmpty()) {
                Text(
                    text = "播放列表为空",
                    modifier = Modifier.padding(20.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(
                        items = tracks,
                        key = { index, track ->
                            "${track.source}-${track.id}-$index"
                        }
                    ) { index, track ->
                        QueueTrackItem(
                            track = track,
                            isCurrentTrack =
                                index == currentIndex,

                            onClick = {
                                onTrackClick(index)
                            },

                            onRemove = {
                                onRemoveTrack(index)
                            }
                        )

                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation = false
            },

            title = {
                Text("清空播放列表")
            },

            text = {
                Text("确定要删除播放列表中的所有歌曲吗？")
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearQueue()
                    }
                ) {
                    Text(
                        text = "清空",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun QueueHeader(
    trackCount: Int,
    onClearClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "当前播放列表",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "共 $trackCount 首歌曲",
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(
            enabled = trackCount > 0,
            onClick = onClearClick
        ) {
            Text(
                text = "清空",
                color = if (trackCount > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭播放列表"
            )
        }
    }
}

@Composable
private fun QueueTrackItem(
    track: Track,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = track.name,
                maxLines = 1,
                color = if (isCurrentTrack) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },

        supportingContent = {
            Text(
                text = if (isCurrentTrack) {
                    "${track.artist} · 正在播放"
                } else {
                    track.artist
                },
                maxLines = 1,
                color = if (isCurrentTrack) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },

        leadingContent = if (isCurrentTrack) {
            {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "正在播放",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            null
        },

        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "从播放列表删除 ${track.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },

        colors = ListItemDefaults.colors(
            containerColor = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),

        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
