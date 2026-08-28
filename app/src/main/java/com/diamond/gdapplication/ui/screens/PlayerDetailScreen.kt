package com.diamond.gdapplication.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.diamond.gdapplication.Track
import java.util.Locale

@Composable
fun PlayerDetailScreen(
    track: Track,
    artworkUrl: String,
    isPlaying: Boolean,
    playbackProgress: Float,
    playbackPositionMs: Long,
    playbackDurationMs: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRequestLyrics: (Track, (Result<Track>) -> Unit) -> Unit
) {
    BackHandler(onBack = onBack)

    var sliderProgress by remember { mutableFloatStateOf(playbackProgress) }
    var isSeeking by remember { mutableStateOf(false) }
    var lyricText by remember(track.source, track.id) { mutableStateOf("") }
    var translatedLyric by remember(track.source, track.id) { mutableStateOf("") }
    var isLoadingLyrics by remember(track.source, track.id) { mutableStateOf(true) }
    var lyricError by remember(track.source, track.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(playbackProgress, isSeeking) {
        if (!isSeeking) sliderProgress = playbackProgress.coerceIn(0f, 1f)
    }

    LaunchedEffect(track.source, track.id, track.lyricId) {
        isLoadingLyrics = true
        lyricError = null
        onRequestLyrics(track) { result ->
            isLoadingLyrics = false
            result.onSuccess { updatedTrack ->
                lyricText = cleanLyrics(updatedTrack.lyric.orEmpty())
                translatedLyric = cleanLyrics(updatedTrack.translatedLyric.orEmpty())
            }
            result.onFailure { error ->
                lyricError = error.message ?: "歌词加载失败"
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("歌曲详情", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "artwork") {
                PlayerArtwork(
                    artworkUrl = artworkUrl,
                    songName = track.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 42.dp, vertical = 16.dp)
                        .aspectRatio(1f)
                )
            }

            item(key = "metadata") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = track.artist.ifBlank { "未知歌手" },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Slider(
                        value = sliderProgress,
                        onValueChange = {
                            isSeeking = true
                            sliderProgress = it
                        },
                        onValueChangeFinished = {
                            if (playbackDurationMs > 0L) {
                                onSeekTo((playbackDurationMs * sliderProgress).toLong())
                            }
                            isSeeking = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )

                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            formatDuration(
                                if (isSeeking && playbackDurationMs > 0L) {
                                    (playbackDurationMs * sliderProgress).toLong()
                                } else {
                                    playbackPositionMs
                                }
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            formatDuration(playbackDurationMs),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSkipPrevious, modifier = Modifier.size(64.dp)) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "上一首",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        IconButton(onClick = onSkipNext, modifier = Modifier.size(64.dp)) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "下一首",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            item(key = "lyrics") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 14.dp,
                        bottom = 40.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("歌词", style = MaterialTheme.typography.titleLarge)
                    when {
                        isLoadingLyrics -> CircularProgressIndicator(
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        lyricError != null -> Text(
                            lyricError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 18.dp)
                        )
                        lyricText.isBlank() && translatedLyric.isBlank() -> Text(
                            "暂无歌词",
                            modifier = Modifier.padding(top = 18.dp)
                        )
                        else -> {
                            if (lyricText.isNotBlank()) {
                                Text(
                                    lyricText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 18.dp)
                                )
                            }
                            if (translatedLyric.isNotBlank()) {
                                Text(
                                    "翻译\n\n$translatedLyric",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerArtwork(
    artworkUrl: String,
    songName: String,
    modifier: Modifier
) {
    if (artworkUrl.isNotBlank() && artworkUrl != "null") {
        AsyncImage(
            model = artworkUrl,
            contentDescription = songName,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(18.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

private fun cleanLyrics(value: String): String = value
    .lineSequence()
    .map { line -> line.replace(LYRIC_TAG, "").trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n")

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return String.format(
        Locale.getDefault(),
        "%d:%02d",
        totalSeconds / 60L,
        totalSeconds % 60L
    )
}

private val LYRIC_TAG = Regex("\\[[^]]*]")
