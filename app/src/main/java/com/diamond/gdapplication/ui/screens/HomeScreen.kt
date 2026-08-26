package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onSongClick: (String) -> Unit
) {
    val demoSongs = listOf(
        "晴天" to "周杰伦",
        "夜曲" to "周杰伦",
        "稻香" to "周杰伦",
        "七里香" to "周杰伦"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenSearch
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )

                Text(
                    text = "搜索歌曲、歌手、专辑或歌单",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
        }

        Text(
            text = "推荐歌曲",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(
                top = 20.dp,
                bottom = 12.dp
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(demoSongs) { song ->
                val name = song.first
                val artist = song.second

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSongClick("$name $artist")
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "播放 $name"
                        )
                    }
                }
            }
        }
    }
}