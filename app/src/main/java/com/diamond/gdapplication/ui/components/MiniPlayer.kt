package com.diamond.gdapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.diamond.gdapplication.MusicController
import com.diamond.gdapplication.Track

@Composable
fun MiniPlayer(
    track: Track?,
    artworkUrl: String,
    isPlaying: Boolean,
    playMode: MusicController.PlayMode,

    // 0f 至 1f
    playbackProgress: Float,

    onPlayPause: () -> Unit,
    onSwitchPlayMode: () -> Unit,
    onOpenQueue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 10.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    artworkUrl = artworkUrl,
                    songName = track?.name
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = track?.name ?: "暂未播放",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )

                    Text(
                        text = track?.artist ?: "请选择一首歌曲",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                // 三种模式分别使用不同图标
                IconButton(
                    onClick = onSwitchPlayMode
                ) {
                    Icon(
                        imageVector = when (playMode) {
                            MusicController.PlayMode.LIST_LOOP ->
                                Icons.Default.Repeat

                            MusicController.PlayMode.SINGLE_LOOP ->
                                Icons.Default.RepeatOne

                            MusicController.PlayMode.RANDOM ->
                                Icons.Default.Shuffle
                        },
                        contentDescription = when (playMode) {
                            MusicController.PlayMode.LIST_LOOP ->
                                "列表循环"

                            MusicController.PlayMode.SINGLE_LOOP ->
                                "单曲循环"

                            MusicController.PlayMode.RANDOM ->
                                "随机播放"
                        }
                    )
                }

                IconButton(
                    enabled = track != null,
                    onClick = onPlayPause
                ) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (isPlaying) {
                            "暂停"
                        } else {
                            "播放"
                        }
                    )
                }

                IconButton(
                    onClick = onOpenQueue
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "当前播放列表"
                    )
                }
            }

            // 沿迷你播放器底部边缘显示进度
            LinearProgressIndicator(
                progress = {
                    playbackProgress.coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumArtwork(
    artworkUrl: String,
    songName: String?
) {
    if (
        artworkUrl.isNotBlank() &&
        artworkUrl != "null"
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = songName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null
            )
        }
    }
}